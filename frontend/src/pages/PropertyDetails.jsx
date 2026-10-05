import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage } from '../services/api';
import { bookingApi } from '../services/bookingApi';
import { notificationApi } from '../services/notificationApi';
import { propertyApi } from '../services/propertyApi';
import { reviewApi } from '../services/reviewApi';

export default function PropertyDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, hasRole } = useAuth();
  const [property, setProperty] = useState(null);
  const [reviews, setReviews] = useState(null);
  const [reviewsError, setReviewsError] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [bookingForm, setBookingForm] = useState({ checkInDate: '', checkOutDate: '', numberOfGuests: 1 });
  const [availability, setAvailability] = useState(null);
  const [checking, setChecking] = useState(false);
  const [booking, setBooking] = useState(false);
  const today = new Date().toISOString().slice(0, 10);
  const nights = useMemo(() => {
    if (!bookingForm.checkInDate || !bookingForm.checkOutDate) return 0;
    return Math.round((new Date(`${bookingForm.checkOutDate}T00:00:00`) - new Date(`${bookingForm.checkInDate}T00:00:00`)) / 86400000);
  }, [bookingForm.checkInDate, bookingForm.checkOutDate]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    Promise.allSettled([
      propertyApi.get(id),
      reviewApi.forProperty(id, { page: 0, size: 5, sort: 'newest' }),
    ]).then(([propertyResult, reviewResult]) => {
      if (!active) return;
      if (propertyResult.status === 'rejected') {
        setError(getApiErrorMessage(propertyResult.reason, 'This stay could not be found.'));
      } else {
        setProperty(propertyResult.value.data);
        setError('');
      }
      setReviews(reviewResult.status === 'fulfilled' ? reviewResult.value.data : null);
      setReviewsError(reviewResult.status === 'rejected'
        ? getApiErrorMessage(reviewResult.reason, 'Guest reviews could not be loaded.') : '');
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [id]);

  function updateBookingForm(field, value) {
    setBookingForm((current) => ({ ...current, [field]: value }));
    setAvailability(null);
  }

  async function checkAvailability(event) {
    event.preventDefault();
    if (nights < 1) {
      toast.error('Check-out must be after check-in.');
      return;
    }
    if (Number(bookingForm.numberOfGuests) < 1 || Number(bookingForm.numberOfGuests) > property.maxGuests) {
      toast.error(`This stay accommodates up to ${property.maxGuests} guests.`);
      return;
    }
    setChecking(true);
    try {
      const { data } = await bookingApi.availability({
        propertyId: property.id,
        checkInDate: bookingForm.checkInDate,
        checkOutDate: bookingForm.checkOutDate,
        numberOfGuests: Number(bookingForm.numberOfGuests),
      });
      setAvailability(data);
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Availability could not be checked.'));
      setAvailability(null);
    } finally {
      setChecking(false);
    }
  }

  async function createBooking() {
    if (!user) {
      navigate('/login', { state: { from: { pathname: `/properties/${id}` } } });
      return;
    }
    if (!hasRole('CUSTOMER')) {
      toast.error('Bookings can only be created by customer accounts.');
      return;
    }
    setBooking(true);
    try {
      const { data } = await bookingApi.create({
        propertyId: property.id,
        checkInDate: bookingForm.checkInDate,
        checkOutDate: bookingForm.checkOutDate,
        numberOfGuests: Number(bookingForm.numberOfGuests),
      });
      try {
        await notificationApi.create({ type: 'BOOKING_CREATED', message: `Booking #${data.id} was created and is awaiting confirmation.` });
      } catch {
        toast.info('Your booking was created, but its inbox update could not be saved.');
      }
      navigate(`/checkout/${data.id}`);
    } catch (requestError) {
      setAvailability(null);
      toast.error(getApiErrorMessage(requestError, 'We could not create this booking. Please check dates and try again.'));
    } finally {
      setBooking(false);
    }
  }

  if (loading) return <main className="container listing-page"><div className="loading-row" role="status">Opening this stay…</div></main>;
  if (error || !property) return <main className="container listing-page">
    <div className="empty-panel"><span>⌕</span><h1>We can’t find that stay.</h1><p>{error || 'The property may have been removed.'}</p>
      <Link to="/properties" className="btn btn-dark rounded-pill">Back to stays</Link></div></main>;

  const location = [property.city, property.state, property.country].filter(Boolean).join(', ');
  return (
    <main className="container property-detail-page">
      <div className="detail-breadcrumb"><Link to="/properties">Stays</Link><span> / </span>{location}</div>
      <div className="detail-title-row"><div><p className="eyebrow">{property.propertyType} · {location}</p>
        <h1>{property.title}</h1><p className="text-muted">{property.maxGuests} guests · {property.bedrooms ?? 0} bedrooms · {property.bathrooms ?? 0} bathrooms</p></div>
        {reviews && <div className="detail-rating"><span>★</span><strong>{reviews.reviewCount ? Number(reviews.averageRating).toFixed(1) : 'New'}</strong>
          <a href="#reviews">{reviews.reviewCount} reviews</a></div>}</div>
      <div className="detail-gallery">
        <div className="gallery-main photo-coast"><span className="photo-illustration">⌂</span><span className="gallery-chip">A StayNest favourite</span></div>
        <div className="gallery-side photo-cabin"><span className="photo-illustration">⌁</span></div>
        <div className="gallery-side photo-villa"><span className="photo-illustration">◒</span></div>
      </div>
      <div className="detail-content-grid">
        <div className="detail-main">
          <section className="detail-host-row"><div><p className="eyebrow">A PLACE TO SETTLE IN</p><h2>Hosted with care</h2><p className="small mb-0">A StayNest member is here to welcome you.</p></div>
            <div className="host-avatar">{(property.title || 'S').slice(0, 1).toUpperCase()}</div></section>
          <section className="detail-copy"><h2>About this stay</h2><p>{property.description || 'A thoughtfully prepared home for your next escape. Settle in, slow down, and explore the neighbourhood at your own pace.'}</p></section>
          <section className="detail-copy"><h2>What this place offers</h2>
            {property.amenities?.length ? <div className="amenities-grid">{property.amenities.map((amenity) => <span key={amenity}>✦&nbsp; {amenity}</span>)}</div>
              : <p className="text-muted">Your host is still adding the finishing touches to the amenity list.</p>}</section>
          <section className="detail-copy" id="reviews"><h2>Guest notes <span className="review-count">{reviews?.reviewCount || 0}</span></h2>
            {reviews?.reviewCount > 0 && <p className="review-average">★ {Number(reviews.averageRating).toFixed(1)} average rating</p>}
            {reviewsError && <div className="alert alert-warning" role="alert">{reviewsError}</div>}
            {reviews?.content?.length ? <div className="review-list">{reviews.content.map((review) => <article key={review.id} className="review-card">
              <div className="review-card-header"><strong>Guest #{review.customerId}</strong><span>{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}</span></div>
              <h3>{review.title}</h3><p>{review.comment}</p><small>{new Date(review.createdAt).toLocaleDateString()}</small>
            </article>)}</div> : <p className="text-muted">No guest notes yet. Be the first to share a story after your stay.</p>}</section>
        </div>
        <aside className="booking-card">
          <div className="booking-price"><strong>₹{Number(property.pricePerNight).toLocaleString('en-IN')}</strong><span> / night</span></div>
          <form onSubmit={checkAvailability}>
            <div className="date-fields">
              <label>CHECK IN<input className="form-control" type="date" min={today} required value={bookingForm.checkInDate}
                onChange={(event) => updateBookingForm('checkInDate', event.target.value)} /></label>
              <label>CHECK OUT<input className="form-control" type="date" min={bookingForm.checkInDate || today} required value={bookingForm.checkOutDate}
                onChange={(event) => updateBookingForm('checkOutDate', event.target.value)} /></label>
            </div>
            <label className="guest-label">GUESTS<select className="form-select" value={bookingForm.numberOfGuests}
              onChange={(event) => updateBookingForm('numberOfGuests', event.target.value)}>
              {Array.from({ length: Math.max(1, property.maxGuests || 1) }, (_, index) => <option key={index + 1} value={index + 1}>{index + 1} {index ? 'guests' : 'guest'}</option>)}
            </select></label>
            <button className="btn btn-dark rounded-pill w-100 py-3 mt-3" disabled={checking || !bookingForm.checkInDate || !bookingForm.checkOutDate}>
              {checking ? 'Checking dates…' : 'Check availability'}
            </button>
          </form>
          {availability && <div className={`availability-message ${availability.available ? 'available' : 'unavailable'}`} role="status">
            {availability.available ? <><strong>Your dates are available.</strong><div className="price-breakdown"><span>₹{Number(availability.pricePerNight).toLocaleString('en-IN')} × {availability.numberOfNights} nights</span><strong>₹{Number(availability.estimatedTotal).toLocaleString('en-IN')}</strong></div>
              <button className="btn btn-accent rounded-pill w-100 mt-3" onClick={createBooking} disabled={booking}>{booking ? 'Reserving…' : user ? 'Reserve this stay' : 'Log in to reserve'}</button></>
              : <strong>Those dates or guest count aren’t available. Try another selection.</strong>}
          </div>}
          <p className="booking-footnote">You won’t be charged now. Secure mock payment follows after booking.</p>
          <div className="booking-host-note"><span>⌂</span><span>Every stay is locally hosted and independently managed.</span></div>
        </aside>
      </div>
    </main>
  );
}
