import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext';
import PropertyCard from '../components/PropertyCard';
import { getApiErrorMessage } from '../services/api';
import { bookingApi } from '../services/bookingApi';
import { notificationApi } from '../services/notificationApi';
import { paymentApi } from '../services/paymentApi';
import { propertyApi } from '../services/propertyApi';
import { reviewApi } from '../services/reviewApi';

export default function CustomerDashboard() {
  const { user } = useAuth();
  const [bookings, setBookings] = useState([]);
  const [payments, setPayments] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [properties, setProperties] = useState({});
  const [unread, setUnread] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [panelErrors, setPanelErrors] = useState([]);
  const [reviewDraft, setReviewDraft] = useState(null);
  const [reviewForm, setReviewForm] = useState({ rating: 5, title: '', comment: '' });

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    const [bookingResult, paymentResult, reviewResult, notificationResult] = await Promise.allSettled([
      bookingApi.mine(), paymentApi.mine(), reviewApi.mine(), notificationApi.mine({ page: 0, size: 1 }),
    ]);
    const failedPanels = [
      ['payments', paymentResult], ['reviews', reviewResult], ['notifications', notificationResult],
    ].filter(([, result]) => result.status === 'rejected')
      .map(([panel, result]) => `${panel[0].toUpperCase()}${panel.slice(1)}: ${getApiErrorMessage(result.reason)}`);
    setPanelErrors(failedPanels);
    if (bookingResult.status === 'fulfilled') {
      const userBookings = bookingResult.value.data || [];
      setBookings(userBookings);
      const propertyResults = await Promise.allSettled(userBookings.map((booking) => propertyApi.get(booking.propertyId)));
      const propertyLookup = {};
      propertyResults.forEach((result, index) => {
        if (result.status === 'fulfilled') propertyLookup[userBookings[index].propertyId] = result.value.data;
      });
      setProperties(propertyLookup);
    } else setError(getApiErrorMessage(bookingResult.reason, 'Your bookings could not be loaded.'));
    if (paymentResult.status === 'fulfilled') setPayments(paymentResult.value.data || []);
    if (reviewResult.status === 'fulfilled') setReviews(reviewResult.value.data || []);
    if (notificationResult.status === 'fulfilled') setUnread(notificationResult.value.data.unreadCount || 0);
    setLoading(false);
  }, []);

  useEffect(() => { loadDashboard(); }, [loadDashboard]);

  async function cancelBooking(booking) {
    if (!window.confirm('Cancel this booking?')) return;
    try {
      await bookingApi.cancel(booking.id);
      toast.success('Booking cancelled.');
      try {
        await notificationApi.create({ type: 'BOOKING_CANCELLED', message: `Booking #${booking.id} was cancelled.` });
      } catch {
        toast.info('Your booking was cancelled, but its inbox update could not be saved.');
      }
      await loadDashboard();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'This booking could not be cancelled.'));
    }
  }

  async function refundPayment(payment) {
    if (!window.confirm('Request a mock refund for this payment?')) return;
    try {
      await paymentApi.refund(payment.id);
      toast.success('Mock refund processed.');
      try {
        await notificationApi.create({ type: 'REFUND_SUCCESS', message: `A mock refund was processed for payment #${payment.id}.` });
      } catch {
        toast.info('The refund was processed, but its inbox update could not be saved.');
      }
      await loadDashboard();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'This refund could not be processed.'));
    }
  }

  function openReview(booking) {
    const existing = reviews.find((review) => review.bookingId === booking.id);
    setReviewDraft({ booking, existing });
    setReviewForm(existing
      ? { rating: existing.rating, title: existing.title, comment: existing.comment || '' }
      : { rating: 5, title: '', comment: '' });
  }

  async function saveReview(event) {
    event.preventDefault();
    try {
      if (reviewDraft.existing) await reviewApi.update(reviewDraft.existing.id, reviewForm);
      else {
        await reviewApi.create({
        ...reviewForm,
        propertyId: reviewDraft.booking.propertyId,
        bookingId: reviewDraft.booking.id,
        });
        try {
          await notificationApi.create({ type: 'REVIEW_CREATED', message: `Your review for property #${reviewDraft.booking.propertyId} was shared.` });
        } catch {
          toast.info('Your review was saved, but its inbox update could not be saved.');
        }
      }
      toast.success(reviewDraft.existing ? 'Review updated.' : 'Thanks for sharing your stay.');
      setReviewDraft(null);
      await loadDashboard();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'The review could not be saved.'));
    }
  }

  async function deleteReview(review) {
    if (!window.confirm('Delete your review?')) return;
    try {
      await reviewApi.remove(review.id);
      toast.success('Review removed.');
      await loadDashboard();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'The review could not be deleted.'));
    }
  }

  const completedReviewBookingIds = new Set(reviews.map((review) => review.bookingId));
  const upcoming = bookings.filter((booking) => booking.checkOutDate >= new Date().toISOString().slice(0, 10)
    && booking.status !== 'CANCELLED');
  const pastBookings = bookings.filter((booking) => booking.checkOutDate < new Date().toISOString().slice(0, 10)
    || booking.status === 'CANCELLED');

  return <main className="container dashboard-page">
    <header className="dashboard-welcome"><div><p className="eyebrow">YOUR STAYNEST</p><h1>Good to see you, <em>{user?.name?.split(' ')[0] || 'traveller'}.</em></h1>
      <p>All the details for your next little escape, in one place.</p></div>
      <div className="dashboard-welcome-actions"><Link to="/profile" className="btn btn-outline-dark rounded-pill">Edit profile</Link>
        <Link to="/notifications" className="notification-summary">♧ <span>{unread} unread</span></Link></div></header>
    {error && <div className="alert alert-danger" role="alert">{error}<button className="btn btn-sm btn-outline-danger ms-2" onClick={loadDashboard}>Retry</button></div>}
    {panelErrors.length > 0 && <div className="alert alert-warning" role="alert">{panelErrors.join(' · ')}</div>}
    {loading ? <div className="loading-row">Gathering your trip details…</div> : <>
      <section className="dashboard-section"><div className="section-heading compact"><div><p className="eyebrow">UP NEXT</p><h2>Your upcoming stays</h2></div></div>
        {upcoming.length ? <div className="trip-list">{upcoming.map((booking) => <article className="trip-card" key={booking.id}>
          <div className={`trip-art ${booking.status === 'CANCELLED' ? 'muted' : ''}`}><span>⌂</span></div>
          <div className="trip-content"><div className="trip-header"><span className={`status-pill status-${booking.status.toLowerCase()}`}>{booking.status}</span>
            <span className="small text-muted">Booking #{booking.id}</span></div>
            <h3>{properties[booking.propertyId]?.title || `Stay #${booking.propertyId}`}</h3>
            <p>{properties[booking.propertyId]?.city || 'StayNest property'} · {booking.checkInDate} – {booking.checkOutDate}</p>
            <div className="d-flex flex-wrap gap-2 align-items-center mt-3">
              <span className="payment-status">{booking.paymentStatus}</span>
              {booking.paymentStatus === 'UNPAID' && booking.status !== 'CANCELLED' && <Link className="btn btn-dark btn-sm rounded-pill px-3" to={`/checkout/${booking.id}`}>Pay securely (demo)</Link>}
              {['PENDING', 'CONFIRMED'].includes(booking.status) && <button className="btn btn-outline-dark btn-sm rounded-pill px-3" onClick={() => cancelBooking(booking)}>Cancel booking</button>}
              {booking.status === 'COMPLETED' && <button className="btn btn-outline-dark btn-sm rounded-pill px-3" onClick={() => openReview(booking)}>
                {completedReviewBookingIds.has(booking.id) ? 'Edit review' : 'Write a review'}</button>}
            </div>
          </div><div className="trip-total"><span>Total</span><strong>₹{Number(booking.totalAmount).toLocaleString('en-IN')}</strong></div>
        </article>)}</div> : <div className="empty-panel compact-empty"><span>⌂</span><h3>No trips on the calendar yet.</h3>
          <p>Find a thoughtful place to stay and your plans will show up here.</p><Link to="/properties" className="btn btn-dark rounded-pill">Explore stays</Link></div>}
      </section>

      <section className="dashboard-section"><div className="section-heading compact"><div><p className="eyebrow">THE MILES BEHIND YOU</p><h2>Past stays</h2></div></div>
        {pastBookings.length ? <div className="table-responsive dashboard-table-wrap"><table className="table align-middle dashboard-table">
          <thead><tr><th>Stay</th><th>Dates</th><th>Booking status</th><th>Payment</th><th>Review</th></tr></thead><tbody>
            {pastBookings.map((booking) => <tr key={booking.id}><td>{properties[booking.propertyId]?.title || `Stay #${booking.propertyId}`}</td>
              <td>{booking.checkInDate} – {booking.checkOutDate}</td><td><span className={`status-pill status-${booking.status.toLowerCase()}`}>{booking.status}</span></td>
              <td>{booking.paymentStatus}</td><td>{booking.status === 'COMPLETED' &&
                <button className="btn btn-sm btn-outline-dark rounded-pill" onClick={() => openReview(booking)}>
                  {completedReviewBookingIds.has(booking.id) ? 'Edit review' : 'Write review'}
                </button>}</td></tr>)}
          </tbody></table></div> : <p className="text-muted">Your past trips will be saved here.</p>}
      </section>

      <section className="dashboard-section"><div className="section-heading compact"><div><p className="eyebrow">PAYMENTS</p><h2>Your payment activity</h2></div></div>
        {payments.length ? <div className="table-responsive dashboard-table-wrap"><table className="table align-middle dashboard-table"><thead><tr><th>Reference</th><th>Booking</th><th>Amount</th><th>Status</th><th /></tr></thead><tbody>
          {payments.map((payment) => <tr key={payment.id}><td>{payment.transactionReference || `Payment #${payment.id}`}</td><td>#{payment.bookingId}</td>
            <td>₹{Number(payment.amount).toLocaleString('en-IN')} {payment.currency}</td><td><span className={`status-pill status-${payment.status.toLowerCase()}`}>{payment.status}</span></td>
            <td>{payment.status === 'SUCCESS' && <button className="btn btn-sm btn-outline-dark rounded-pill" onClick={() => refundPayment(payment)}>Request mock refund</button>}</td></tr>)}
        </tbody></table></div> : <p className="text-muted">Payments will appear here when you book a stay.</p>}
      </section>

      <section className="dashboard-section"><div className="section-heading compact"><div><p className="eyebrow">YOUR WORDS</p><h2>Reviews you’ve shared</h2></div></div>
        {reviews.length ? <div className="owned-review-grid">{reviews.map((review) => <article className="owned-review" key={review.id}>
          <div className="review-card-header"><strong>Stay #{review.propertyId}</strong><span>{'★'.repeat(review.rating)}</span></div><h3>{review.title}</h3><p>{review.comment}</p>
          <div className="d-flex gap-2"><button className="btn btn-sm btn-outline-dark rounded-pill" onClick={() => {
            const booking = bookings.find((item) => item.id === review.bookingId);
            if (booking) openReview(booking);
          }}>Edit</button><button className="btn btn-sm btn-outline-danger rounded-pill" onClick={() => deleteReview(review)}>Delete</button></div>
        </article>)}</div> : <p className="text-muted">After a completed stay, share a few words to help the next guest.</p>}
      </section>
    </>}
    {reviewDraft && <div className="modal-backdrop-custom" role="presentation" onClick={() => setReviewDraft(null)}>
      <section className="review-modal" role="dialog" aria-modal="true" aria-labelledby="reviewModalTitle" onClick={(event) => event.stopPropagation()}>
        <button className="modal-close" aria-label="Close" onClick={() => setReviewDraft(null)}>×</button>
        <p className="eyebrow">A NOTE FOR THE NEXT GUEST</p><h2 id="reviewModalTitle">{reviewDraft.existing ? 'Edit your review' : 'How was your stay?'}</h2>
        <form onSubmit={saveReview}><label className="form-label">Your rating<select className="form-select mb-3" value={reviewForm.rating}
          onChange={(event) => setReviewForm({ ...reviewForm, rating: Number(event.target.value) })}>{[5, 4, 3, 2, 1].map((rating) => <option key={rating} value={rating}>{rating} / 5 stars</option>)}</select></label>
          <label className="form-label">Title<input className="form-control mb-3" maxLength="120" required value={reviewForm.title}
            onChange={(event) => setReviewForm({ ...reviewForm, title: event.target.value })} /></label>
          <label className="form-label">Your note<textarea className="form-control mb-3" rows="4" maxLength="2000" value={reviewForm.comment}
            onChange={(event) => setReviewForm({ ...reviewForm, comment: event.target.value })} /></label>
          <button className="btn btn-dark rounded-pill w-100">Save review</button>
        </form>
      </section>
    </div>}
    <section className="dashboard-section"><p className="eyebrow">A PLACE TO GO NEXT</p><h2>Still dreaming?</h2>
      <div className="row g-4 mt-1">{Object.values(properties).slice(0, 3).map((property, index) =>
        <div className="col-md-4" key={property.id}><PropertyCard property={property} index={index} /></div>)}</div>
    </section>
  </main>;
}
