import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage } from '../services/api';
import { bookingApi } from '../services/bookingApi';
import { notificationApi } from '../services/notificationApi';
import { paymentApi } from '../services/paymentApi';

const paymentMethods = ['MOCK', 'CARD', 'UPI', 'NET_BANKING'];

export default function Checkout() {
  const { bookingId } = useParams();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const [booking, setBooking] = useState(null);
  const [method, setMethod] = useState('MOCK');
  const [simulateFailure, setSimulateFailure] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  useEffect(() => {
    bookingApi.get(bookingId)
      .then(({ data }) => setBooking(data))
      .catch((requestError) => setError(getApiErrorMessage(requestError, 'Booking details could not be loaded.')))
      .finally(() => setLoading(false));
  }, [bookingId]);

  async function pay(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const { data } = await paymentApi.create({
        bookingId: Number(bookingId),
        currency: 'INR',
        paymentMethod: method,
        simulateFailure,
      });
      setResult(data);
      const succeeded = data.status === 'SUCCESS';
      if (succeeded) toast.success('Demo payment successful.');
      else toast.error('Demo payment failed. No money was charged.');
      try {
        await notificationApi.create({
          type: succeeded ? 'PAYMENT_SUCCESS' : 'PAYMENT_FAILED',
          message: succeeded ? `Mock payment for booking #${booking.id} succeeded.` : `Mock payment for booking #${booking.id} failed; no money was charged.`,
        });
      } catch {
        toast.info('Payment result was recorded, but its inbox update could not be saved.');
      }
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'The demo payment could not be completed.'));
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <main className="container checkout-page"><div className="loading-row">Loading booking…</div></main>;
  if (error && !booking) return <main className="container checkout-page"><div className="alert alert-danger">{error}</div><Link to="/dashboard">Back to dashboard</Link></main>;
  if (!booking) return null;

  return <main className="container checkout-page">
    <Link to="/dashboard" className="back-link">← Your trips</Link>
    <div className="checkout-heading"><p className="eyebrow">ONE LAST STEP</p><h1>Confirm your <em>stay.</em></h1></div>
    <div className="checkout-grid">
      <section className="checkout-main">
        <div className="checkout-box">
          <p className="eyebrow">DEMO PAYMENT ONLY</p>
          <h2>Choose a demo method</h2>
          <div className="mock-warning" role="note"><strong>StayNest mock checkout.</strong> This is a portfolio demo, not a real payment. Never enter card numbers, CVV, UPI PINs, or bank credentials.</div>
          {error && <div className="alert alert-danger mt-3" role="alert">{error}</div>}
          {result ? <div className={`payment-result ${result.status === 'SUCCESS' ? 'success' : 'failure'}`}>
            <span className="result-icon">{result.status === 'SUCCESS' ? '✓' : '×'}</span>
            <h3>{result.status === 'SUCCESS' ? 'You’re all set.' : 'Demo payment not completed.'}</h3>
            <p>{result.status === 'SUCCESS' ? 'Your booking payment is recorded as a demo success.' : 'The failure was simulated. No payment credentials were collected.'}</p>
            <p className="small">Reference: {result.transactionReference}</p>
            <button className="btn btn-dark rounded-pill px-4" onClick={() => navigate('/dashboard')}>Go to your trips</button>
          </div> : <form onSubmit={pay}>
            <div className="payment-methods">{paymentMethods.map((item) => <label className={`method-option ${method === item ? 'selected' : ''}`} key={item}>
              <input type="radio" name="paymentMethod" value={item} checked={method === item} onChange={() => setMethod(item)} />
              <span className="method-icon">{item === 'CARD' ? '▭' : item === 'UPI' ? '↗' : item === 'NET_BANKING' ? '⌂' : '✳'}</span>
              <span><strong>{item.replace('_', ' ')}</strong><small>{item === 'MOCK' ? 'Recommended for the demo' : 'Simulated only'}</small></span>
            </label>)}</div>
            <label className="simulate-option"><input type="checkbox" checked={simulateFailure} onChange={(event) => setSimulateFailure(event.target.checked)} />
              Simulate a failed demo transaction for testing</label>
            <button className="btn btn-dark btn-lg rounded-pill w-100 mt-3" disabled={submitting || !hasRole('CUSTOMER')}>
              {submitting ? 'Processing demo…' : 'Complete demo payment'}
            </button>
            {!hasRole('CUSTOMER') && <p className="small text-danger mt-2">Only the booking customer can pay for this booking.</p>}
          </form>}
        </div>
      </section>
      <aside className="checkout-summary">
        <p className="eyebrow">YOUR BOOKING</p><h2>Stay #{booking.id}</h2>
        <div className="summary-line"><span>Check in</span><strong>{booking.checkInDate}</strong></div>
        <div className="summary-line"><span>Check out</span><strong>{booking.checkOutDate}</strong></div>
        <div className="summary-line"><span>Guests</span><strong>{booking.numberOfGuests}</strong></div>
        <hr /><div className="summary-line total"><span>Total</span><strong>₹{Number(booking.totalAmount).toLocaleString('en-IN')}</strong></div>
        <p className="small text-muted mt-3">The amount is provided by Booking Service. This demo does not transfer money.</p>
      </aside>
    </div>
  </main>;
}
