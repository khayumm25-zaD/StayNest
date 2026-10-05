import { useCallback, useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import { getApiErrorMessage } from '../services/api';
import { authApi } from '../services/authApi';
import { bookingApi } from '../services/bookingApi';
import { notificationApi } from '../services/notificationApi';
import { paymentApi } from '../services/paymentApi';
import { propertyApi } from '../services/propertyApi';
import { reviewApi } from '../services/reviewApi';

const roles = ['CUSTOMER', 'HOST', 'ADMIN'];

export default function AdminDashboard() {
  const [data, setData] = useState({ users: [], properties: [], bookings: [], payments: [], reviews: [], notifications: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editingReview, setEditingReview] = useState(null);
  const [reviewForm, setReviewForm] = useState({ rating: 5, title: '', comment: '' });
  const [editingProperty, setEditingProperty] = useState(null);
  const [propertyForm, setPropertyForm] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    const results = await Promise.allSettled([
      authApi.users(), propertyApi.list(), bookingApi.host(), paymentApi.mine(),
      reviewApi.all({ page: 0, size: 100 }), notificationApi.all({ page: 0, size: 100 }),
    ]);
    const keys = ['users', 'properties', 'bookings', 'payments', 'reviews', 'notifications'];
    const next = { users: [], properties: [], bookings: [], payments: [], reviews: [], notifications: [] };
    let firstError = '';
    results.forEach((result, index) => {
      if (result.status === 'rejected') {
        if (!firstError) firstError = getApiErrorMessage(result.reason, `${keys[index]} could not be loaded.`);
      } else {
        const body = result.value.data;
        next[keys[index]] = Array.isArray(body) ? body : body?.content || [];
      }
    });
    setData(next);
    setError(firstError);
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  async function toggleRole(user, role) {
    const current = user.roles || [];
    const updated = current.includes(role) ? current.filter((item) => item !== role) : [...current, role];
    try {
      await authApi.updateRoles(user.id, updated);
      toast.success(`Roles updated for ${user.name}.`);
      await load();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Role update failed.'));
    }
  }

  function editProperty(property) {
    setEditingProperty(property);
    setPropertyForm({ ...property, amenities: (property.amenities || []).join(', ') });
  }

  async function saveProperty(event) {
    event.preventDefault();
    try {
      await propertyApi.update(editingProperty.id, {
        ...propertyForm,
        pricePerNight: Number(propertyForm.pricePerNight),
        maxGuests: Number(propertyForm.maxGuests),
        bedrooms: Number(propertyForm.bedrooms),
        bathrooms: Number(propertyForm.bathrooms),
        amenities: propertyForm.amenities.split(',').map((value) => value.trim()).filter(Boolean),
      });
      toast.success('Property updated.');
      setEditingProperty(null);
      await load();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Property could not be updated.'));
    }
  }

  async function removeProperty(property) {
    if (!window.confirm(`Delete ${property.title}?`)) return;
    try {
      await propertyApi.remove(property.id);
      toast.success('Property deleted.');
      await load();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Property could not be deleted.'));
    }
  }

  async function changeBooking(booking, status) {
    try {
      await bookingApi.updateStatus(booking.id, status);
      toast.success('Booking status updated.');
      await load();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Booking status could not be updated.'));
    }
  }

  async function cancelBooking(booking) {
    if (!window.confirm(`Cancel booking #${booking.id}?`)) return;
    try {
      await bookingApi.cancel(booking.id);
      toast.success('Booking cancelled.');
      await load();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Booking could not be cancelled.'));
    }
  }

  async function refund(payment) {
    if (!window.confirm('Process this mock refund?')) return;
    try {
      await paymentApi.refund(payment.id);
      toast.success('Mock refund processed.');
      await load();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Refund could not be processed.'));
    }
  }

  async function deleteReview(review) {
    if (!window.confirm('Delete this review?')) return;
    try {
      await reviewApi.remove(review.id);
      toast.success('Review deleted.');
      await load();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Review could not be deleted.'));
    }
  }

  function editReview(review) {
    setEditingReview(review);
    setReviewForm({ rating: review.rating, title: review.title, comment: review.comment || '' });
  }

  async function saveReview(event) {
    event.preventDefault();
    try {
      await reviewApi.update(editingReview.id, reviewForm);
      toast.success('Review updated.');
      setEditingReview(null);
      await load();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Review could not be updated.'));
    }
  }

  async function markNotificationRead(notification) {
    try {
      await notificationApi.markRead(notification.id);
      await load();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Notification could not be updated.'));
    }
  }

  return <main className="container dashboard-page admin-page">
    <header className="dashboard-welcome"><div><p className="eyebrow">STAYNEST CONTROL ROOM</p><h1>Everything, <em>in balance.</em></h1>
      <p>Manage members, stays, trips and the little details that keep things running.</p></div>
      <button className="btn btn-outline-dark rounded-pill" onClick={load}>Refresh overview</button></header>
    {error && <div className="alert alert-warning" role="alert">{error} Some panels may be incomplete.</div>}
    {loading ? <div className="loading-row">Loading the StayNest overview…</div> : <>
      <div className="metric-grid metric-grid-wide">
        <article className="metric-card"><span>MEMBERS</span><strong>{data.users.length}</strong><small>Registered accounts</small></article>
        <article className="metric-card"><span>PROPERTIES</span><strong>{data.properties.length}</strong><small>Places to stay</small></article>
        <article className="metric-card"><span>BOOKINGS</span><strong>{data.bookings.length}</strong><small>Across all stays</small></article>
        <article className="metric-card"><span>PAYMENTS</span><strong>{data.payments.length}</strong><small>Mock transactions</small></article>
        <article className="metric-card"><span>REVIEWS</span><strong>{data.reviews.length}</strong><small>Guest notes</small></article>
      </div>
      <section className="dashboard-section"><div className="section-heading compact"><div><p className="eyebrow">PEOPLE</p><h2>Accounts & roles</h2></div></div>
        <div className="table-responsive dashboard-table-wrap"><table className="table align-middle dashboard-table"><thead><tr><th>Member</th><th>Email</th><th>Joined roles</th><th>Role controls</th></tr></thead><tbody>
          {data.users.map((member) => <tr key={member.id}><td>{member.name}</td><td>{member.email}</td><td>{(member.roles || []).join(', ')}</td>
            <td><div className="d-flex flex-wrap gap-1">{roles.map((role) => <button key={role}
              className={`btn btn-sm rounded-pill ${member.roles?.includes(role) ? 'btn-dark' : 'btn-outline-secondary'}`}
              onClick={() => toggleRole(member, role)}>{member.roles?.includes(role) ? '✓ ' : '+ '}{role}</button>)}</div></td></tr>)}
        </tbody></table></div>
      </section>
      <section className="dashboard-section"><div className="section-heading compact"><div><p className="eyebrow">STAYS</p><h2>Property directory</h2></div></div>
        <div className="table-responsive dashboard-table-wrap"><table className="table align-middle dashboard-table"><thead><tr><th>Property</th><th>Location</th><th>Host</th><th>Price / night</th><th>Action</th></tr></thead><tbody>
          {data.properties.map((property) => <tr key={property.id}><td>{property.title}</td><td>{[property.city, property.country].filter(Boolean).join(', ')}</td>
            <td>#{property.hostId}</td><td>₹{Number(property.pricePerNight).toLocaleString('en-IN')}</td><td>
              <div className="d-flex gap-1"><button className="btn btn-sm btn-outline-dark rounded-pill" onClick={() => editProperty(property)}>Edit</button>
                <button className="btn btn-sm btn-outline-danger rounded-pill" onClick={() => removeProperty(property)}>Delete</button></div></td></tr>)}
        </tbody></table></div>
      </section>
      <section className="dashboard-section"><div className="section-heading compact"><div><p className="eyebrow">TRAVEL</p><h2>Bookings</h2></div></div>
        <div className="table-responsive dashboard-table-wrap"><table className="table align-middle dashboard-table"><thead><tr><th>Booking</th><th>Property</th><th>Customer</th><th>Dates</th><th>Status</th><th>Action</th></tr></thead><tbody>
          {data.bookings.map((booking) => <tr key={booking.id}><td>#{booking.id}</td><td>#{booking.propertyId}</td><td>#{booking.customerId}</td>
            <td>{booking.checkInDate} – {booking.checkOutDate}</td><td><span className={`status-pill status-${booking.status.toLowerCase()}`}>{booking.status}</span></td><td>
              {booking.status === 'PENDING' && <button className="btn btn-sm btn-outline-dark rounded-pill" onClick={() => changeBooking(booking, 'CONFIRMED')}>Confirm</button>}
              {booking.status === 'CONFIRMED' && <button className="btn btn-sm btn-outline-dark rounded-pill" onClick={() => changeBooking(booking, 'COMPLETED')}>Complete</button>}
              {['PENDING', 'CONFIRMED'].includes(booking.status) && <button className="btn btn-sm btn-outline-danger rounded-pill ms-1" onClick={() => cancelBooking(booking)}>Cancel</button>}</td></tr>)}
        </tbody></table></div>
      </section>
      <section className="dashboard-section"><div className="section-heading compact"><div><p className="eyebrow">DEMO TRANSACTIONS</p><h2>Payments</h2></div></div>
        <div className="table-responsive dashboard-table-wrap"><table className="table align-middle dashboard-table"><thead><tr><th>Reference</th><th>Customer</th><th>Booking</th><th>Amount</th><th>Status</th><th /></tr></thead><tbody>
          {data.payments.map((payment) => <tr key={payment.id}><td>{payment.transactionReference}</td><td>#{payment.customerId}</td><td>#{payment.bookingId}</td>
            <td>₹{Number(payment.amount).toLocaleString('en-IN')}</td><td>{payment.status}</td><td>{payment.status === 'SUCCESS' &&
              <button className="btn btn-sm btn-outline-dark rounded-pill" onClick={() => refund(payment)}>Mock refund</button>}</td></tr>)}
        </tbody></table></div>
      </section>
      <section className="dashboard-section"><div className="section-heading compact"><div><p className="eyebrow">GUEST NOTES</p><h2>Reviews</h2></div></div>
        <div className="row g-3">{data.reviews.map((review) => <div className="col-md-6 col-lg-4" key={review.id}><article className="owned-review">
          <div className="review-card-header"><strong>Property #{review.propertyId}</strong><span>{'★'.repeat(review.rating)}</span></div>
          <h3>{review.title}</h3><p>{review.comment}</p><small>Booking #{review.bookingId} · Guest #{review.customerId}</small>
          <div className="mt-3 d-flex gap-2"><button className="btn btn-sm btn-outline-dark rounded-pill" onClick={() => editReview(review)}>Edit</button>
            <button className="btn btn-sm btn-outline-danger rounded-pill" onClick={() => deleteReview(review)}>Delete review</button></div>
        </article></div>)}</div>
      </section>
      <section className="dashboard-section"><div className="section-heading compact"><div><p className="eyebrow">SYSTEM INBOX</p><h2>Notifications</h2></div></div>
        <div className="notification-list">{data.notifications.map((notification) => <article className="notification-full" key={notification.id}>
          <span className="notification-type-icon">♧</span><div className="flex-grow-1"><strong>{notification.type.replaceAll('_', ' ')}</strong>
            <p className="mb-0">{notification.message}</p><small className="text-muted">User #{notification.userId} · {new Date(notification.createdAt).toLocaleString()}</small></div>
          {notification.unread && <button className="btn btn-sm btn-outline-dark rounded-pill" onClick={() => markNotificationRead(notification)}>Mark read</button>}
        </article>)}</div>
      </section>
    </>}
    {editingReview && <div className="modal-backdrop-custom" role="presentation" onClick={() => setEditingReview(null)}>
      <section className="review-modal" role="dialog" aria-modal="true" aria-labelledby="adminReviewTitle" onClick={(event) => event.stopPropagation()}>
        <button className="modal-close" aria-label="Close" onClick={() => setEditingReview(null)}>×</button>
        <p className="eyebrow">ADMIN REVIEW TOOLS</p><h2 id="adminReviewTitle">Edit review</h2>
        <form onSubmit={saveReview}><label className="form-label">Rating<select className="form-select mb-3" value={reviewForm.rating}
          onChange={(event) => setReviewForm({ ...reviewForm, rating: Number(event.target.value) })}>{[5, 4, 3, 2, 1].map((rating) => <option value={rating} key={rating}>{rating} / 5</option>)}</select></label>
          <label className="form-label">Title<input className="form-control mb-3" required maxLength="120" value={reviewForm.title}
            onChange={(event) => setReviewForm({ ...reviewForm, title: event.target.value })} /></label>
          <label className="form-label">Comment<textarea className="form-control mb-3" rows="4" maxLength="2000" value={reviewForm.comment}
            onChange={(event) => setReviewForm({ ...reviewForm, comment: event.target.value })} /></label>
          <button className="btn btn-dark rounded-pill w-100">Save changes</button>
        </form>
      </section>
    </div>}
    {editingProperty && propertyForm && <div className="modal-backdrop-custom" role="presentation" onClick={() => setEditingProperty(null)}>
      <section className="review-modal host-property-modal" role="dialog" aria-modal="true" aria-labelledby="adminPropertyTitle" onClick={(event) => event.stopPropagation()}>
        <button className="modal-close" aria-label="Close" onClick={() => setEditingProperty(null)}>×</button>
        <p className="eyebrow">ADMIN PROPERTY TOOLS</p><h2 id="adminPropertyTitle">Edit property</h2>
        <form onSubmit={saveProperty} className="host-form-grid">
          <label className="form-label span-2">Title<input className="form-control" required maxLength="150" value={propertyForm.title}
            onChange={(event) => setPropertyForm({ ...propertyForm, title: event.target.value })} /></label>
          <label className="form-label">Type<input className="form-control" required value={propertyForm.propertyType}
            onChange={(event) => setPropertyForm({ ...propertyForm, propertyType: event.target.value })} /></label>
          <label className="form-label">Status<select className="form-select" value={propertyForm.status || 'ACTIVE'}
            onChange={(event) => setPropertyForm({ ...propertyForm, status: event.target.value })}><option>ACTIVE</option><option>INACTIVE</option></select></label>
          <label className="form-label">Price per night<input className="form-control" type="number" min="0.01" step="0.01" required value={propertyForm.pricePerNight}
            onChange={(event) => setPropertyForm({ ...propertyForm, pricePerNight: event.target.value })} /></label>
          <label className="form-label">Maximum guests<input className="form-control" type="number" min="1" required value={propertyForm.maxGuests}
            onChange={(event) => setPropertyForm({ ...propertyForm, maxGuests: event.target.value })} /></label>
          <label className="form-label">City<input className="form-control" value={propertyForm.city || ''}
            onChange={(event) => setPropertyForm({ ...propertyForm, city: event.target.value })} /></label>
          <label className="form-label">Country<input className="form-control" value={propertyForm.country || ''}
            onChange={(event) => setPropertyForm({ ...propertyForm, country: event.target.value })} /></label>
          <label className="form-label span-2">Amenities<input className="form-control" value={propertyForm.amenities}
            onChange={(event) => setPropertyForm({ ...propertyForm, amenities: event.target.value })} /></label>
          <label className="form-label span-2">Description<textarea className="form-control" rows="3" maxLength="2000" value={propertyForm.description || ''}
            onChange={(event) => setPropertyForm({ ...propertyForm, description: event.target.value })} /></label>
          <button className="btn btn-dark rounded-pill span-2">Save property</button>
        </form>
      </section>
    </div>}
  </main>;
}
