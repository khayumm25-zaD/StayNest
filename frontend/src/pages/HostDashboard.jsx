import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage } from '../services/api';
import { bookingApi } from '../services/bookingApi';
import { notificationApi } from '../services/notificationApi';
import { propertyApi } from '../services/propertyApi';
import { reviewApi } from '../services/reviewApi';

const emptyProperty = {
  title: '', description: '', propertyType: 'Homestay', city: '', state: '', country: 'India',
  pricePerNight: '', maxGuests: 2, bedrooms: 1, bathrooms: 1, amenities: '', status: 'ACTIVE',
};

export default function HostDashboard() {
  const { user } = useAuth();
  const [properties, setProperties] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(emptyProperty);
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    const [propertyResult, bookingResult] = await Promise.allSettled([propertyApi.list(), bookingApi.host()]);
    if (propertyResult.status === 'fulfilled') {
      const owned = (propertyResult.value.data || []).filter((property) => property.hostId === user?.id);
      setProperties(owned);
      const reviewResults = await Promise.allSettled(owned.map((property) =>
        reviewApi.forProperty(property.id, { page: 0, size: 50, sort: 'newest' })));
      setReviews(reviewResults.flatMap((result) => result.status === 'fulfilled' ? result.value.data.content || [] : []));
    } else setError(getApiErrorMessage(propertyResult.reason, 'Your listings could not be loaded.'));
    if (bookingResult.status === 'fulfilled') setBookings(bookingResult.value.data || []);
    else setError(getApiErrorMessage(bookingResult.reason, 'Your property bookings could not be loaded.'));
    setLoading(false);
  }, [user?.id]);

  useEffect(() => { load(); }, [load]);

  function startCreate() {
    setEditingId(null);
    setForm(emptyProperty);
    setShowForm(true);
  }

  function startEdit(property) {
    setEditingId(property.id);
    setForm({ ...property, pricePerNight: property.pricePerNight, amenities: (property.amenities || []).join(', ') });
    setShowForm(true);
  }

  async function saveProperty(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    const payload = {
      ...form,
      pricePerNight: Number(form.pricePerNight),
      maxGuests: Number(form.maxGuests),
      bedrooms: Number(form.bedrooms),
      bathrooms: Number(form.bathrooms),
      amenities: form.amenities.split(',').map((value) => value.trim()).filter(Boolean),
    };
    try {
      if (editingId) await propertyApi.update(editingId, payload);
      else await propertyApi.create(payload);
      toast.success(editingId ? 'Listing updated.' : 'Your property is live.');
      setShowForm(false);
      await load();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'The listing could not be saved.'));
    } finally {
      setSaving(false);
    }
  }

  async function removeProperty(property) {
    if (!window.confirm(`Delete “${property.title}”?`)) return;
    try {
      await propertyApi.remove(property.id);
      toast.success('Listing deleted.');
      await load();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'The listing could not be deleted.'));
    }
  }

  async function changeStatus(booking, status) {
    try {
      await bookingApi.updateStatus(booking.id, status);
      toast.success(`Booking ${status.toLowerCase()}.`);
      if (status === 'CONFIRMED') {
        try {
          await notificationApi.create({ type: 'BOOKING_CONFIRMED', message: `Booking #${booking.id} was confirmed.` });
        } catch {
          toast.info('Booking was confirmed, but its inbox update could not be saved.');
        }
      }
      await load();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Booking status could not be updated.'));
    }
  }

  const propertyIds = new Set(properties.map((property) => property.id));
  const hostBookings = bookings.filter((booking) => propertyIds.has(booking.propertyId));

  return <main className="container dashboard-page">
    <header className="dashboard-welcome"><div><p className="eyebrow">HOST SPACE</p><h1>Your homes, <em>your way.</em></h1>
      <p>Welcome, {user?.name}. Keep your listings fresh and your guests in the loop.</p></div>
      <button className="btn btn-dark rounded-pill px-4" onClick={startCreate}>＋ Add a property</button></header>
    {error && <div className="alert alert-danger" role="alert">{error}<button className="btn btn-sm btn-outline-danger ms-2" onClick={load}>Retry</button></div>}
    <div className="metric-grid">
      <article className="metric-card"><span>YOUR LISTINGS</span><strong>{properties.length}</strong><small>Homes managed by you</small></article>
      <article className="metric-card"><span>GUEST BOOKINGS</span><strong>{hostBookings.length}</strong><small>Across your properties</small></article>
      <article className="metric-card"><span>NEEDS YOUR ATTENTION</span><strong>{hostBookings.filter((booking) => booking.status === 'PENDING').length}</strong><small>Pending confirmations</small></article>
    </div>
    <section className="dashboard-section"><div className="section-heading compact"><div><p className="eyebrow">YOUR COLLECTION</p><h2>Property listings</h2></div>
      <button className="btn btn-outline-dark rounded-pill" onClick={startCreate}>Add a stay</button></div>
      {loading ? <div className="loading-row">Loading your homes…</div>
        : properties.length ? <div className="row g-4">{properties.map((property, index) => <div className="col-md-6 col-lg-4" key={property.id}>
          <div className="host-property-card"><div className={`property-photo photo-${['coast', 'cabin', 'villa'][index % 3]}`}><span className="photo-illustration">⌂</span></div>
            <div className="p-3"><span className="status-pill status-confirmed">{property.status || 'ACTIVE'}</span><h3 className="mt-2">{property.title}</h3>
              <p className="text-muted">{property.city}, {property.country} · ₹{Number(property.pricePerNight).toLocaleString('en-IN')}/night</p>
              <div className="d-flex gap-2"><Link className="btn btn-sm btn-outline-dark rounded-pill" to={`/properties/${property.id}`}>Preview</Link>
                <button className="btn btn-sm btn-outline-dark rounded-pill" onClick={() => startEdit(property)}>Edit</button>
                <button className="btn btn-sm btn-outline-danger rounded-pill" onClick={() => removeProperty(property)}>Delete</button></div>
            </div></div></div>)}</div>
          : !error && <div className="empty-panel"><span>⌂</span><h3>Your first listing starts here.</h3><p>Share a home you love with someone ready to explore.</p><button className="btn btn-dark rounded-pill" onClick={startCreate}>Add a property</button></div>}
    </section>
    <section className="dashboard-section"><div className="section-heading compact"><div><p className="eyebrow">UPCOMING GUESTS</p><h2>Property bookings</h2></div></div>
      {hostBookings.length ? <div className="table-responsive dashboard-table-wrap"><table className="table align-middle dashboard-table"><thead><tr><th>Booking</th><th>Property</th><th>Dates</th><th>Guests</th><th>Status</th><th>Next step</th></tr></thead><tbody>
        {hostBookings.map((booking) => <tr key={booking.id}><td>#{booking.id}</td><td>{properties.find((property) => property.id === booking.propertyId)?.title || `Stay #${booking.propertyId}`}</td>
          <td>{booking.checkInDate} – {booking.checkOutDate}</td><td>{booking.numberOfGuests}</td><td><span className={`status-pill status-${booking.status.toLowerCase()}`}>{booking.status}</span></td>
          <td>{booking.status === 'PENDING' && <button className="btn btn-sm btn-dark rounded-pill" onClick={() => changeStatus(booking, 'CONFIRMED')}>Confirm</button>}
            {booking.status === 'CONFIRMED' && <button className="btn btn-sm btn-outline-dark rounded-pill" onClick={() => changeStatus(booking, 'COMPLETED')}>Mark completed</button>}
            {!['PENDING', 'CONFIRMED'].includes(booking.status) && '—'}</td></tr>)}
      </tbody></table></div> : <p className="text-muted">Bookings for your homes will appear here.</p>}
    </section>
    <section className="dashboard-section"><div className="section-heading compact"><div><p className="eyebrow">GUEST NOTES</p><h2>Reviews for your stays</h2></div></div>
      {reviews.length ? <div className="owned-review-grid">{reviews.map((review) => <article className="owned-review" key={review.id}>
        <div className="review-card-header"><strong>Property #{review.propertyId}</strong><span>{'★'.repeat(review.rating)}</span></div>
        <h3>{review.title}</h3><p>{review.comment}</p><small>Booking #{review.bookingId}</small>
      </article>)}</div> : <p className="text-muted">Guest reviews will appear here after completed stays.</p>}
    </section>
    {showForm && <div className="modal-backdrop-custom" role="presentation" onClick={() => setShowForm(false)}>
      <section className="review-modal host-property-modal" role="dialog" aria-modal="true" aria-labelledby="propertyFormTitle" onClick={(event) => event.stopPropagation()}>
        <button className="modal-close" aria-label="Close" onClick={() => setShowForm(false)}>×</button>
        <p className="eyebrow">HOST YOUR HOME</p><h2 id="propertyFormTitle">{editingId ? 'Edit your listing' : 'Add a new stay'}</h2>
        {error && <div className="alert alert-danger">{error}</div>}
        <form onSubmit={saveProperty} className="host-form-grid">
          <label className="form-label span-2">Listing title<input className="form-control" maxLength="150" required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label>
          <label className="form-label">Stay type<input className="form-control" maxLength="80" required value={form.propertyType} onChange={(e) => setForm({ ...form, propertyType: e.target.value })} /></label>
          <label className="form-label">Nightly price (₹)<input className="form-control" type="number" min="0.01" step="0.01" required value={form.pricePerNight} onChange={(e) => setForm({ ...form, pricePerNight: e.target.value })} /></label>
          <label className="form-label">City<input className="form-control" value={form.city || ''} onChange={(e) => setForm({ ...form, city: e.target.value })} /></label>
          <label className="form-label">State<input className="form-control" value={form.state || ''} onChange={(e) => setForm({ ...form, state: e.target.value })} /></label>
          <label className="form-label">Country<input className="form-control" value={form.country || ''} onChange={(e) => setForm({ ...form, country: e.target.value })} /></label>
          <label className="form-label">Max guests<input className="form-control" type="number" min="1" required value={form.maxGuests} onChange={(e) => setForm({ ...form, maxGuests: e.target.value })} /></label>
          <label className="form-label">Bedrooms<input className="form-control" type="number" min="0" value={form.bedrooms} onChange={(e) => setForm({ ...form, bedrooms: e.target.value })} /></label>
          <label className="form-label">Bathrooms<input className="form-control" type="number" min="0" value={form.bathrooms} onChange={(e) => setForm({ ...form, bathrooms: e.target.value })} /></label>
          <label className="form-label span-2">Amenities, separated by commas<input className="form-control" value={form.amenities} onChange={(e) => setForm({ ...form, amenities: e.target.value })} /></label>
          <label className="form-label span-2">Description<textarea className="form-control" rows="3" maxLength="2000" value={form.description || ''} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
          <button className="btn btn-dark rounded-pill span-2" disabled={saving}>{saving ? 'Saving…' : editingId ? 'Save changes' : 'Publish property'}</button>
        </form>
      </section>
    </div>}
  </main>;
}
