import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage } from '../services/api';

export default function Register() {
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const { register } = useAuth();
  const navigate = useNavigate();

  async function submit(event) {
    event.preventDefault();
    setError('');
    if (form.password.length < 6) {
      setError('Use a password with at least 6 characters.');
      return;
    }
    setSubmitting(true);
    try {
      await register(form);
      toast.success('Your account is ready.');
      navigate('/dashboard', { replace: true });
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'We could not create your account.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-shell">
      <section className="auth-card">
        <Link className="staynest-brand mb-4 d-inline-block" to="/">StayNest<span>.</span></Link>
        <p className="eyebrow">MAKE YOURSELF AT HOME</p>
        <h1 className="h2 fw-bold mb-2">Create your account</h1>
        <p className="text-muted mb-4">Join a community that travels a little more personally.</p>
        {error && <div className="alert alert-danger" role="alert">{error}</div>}
        <form onSubmit={submit}>
          <label className="form-label" htmlFor="name">Full name</label>
          <input id="name" className="form-control form-control-lg mb-3" autoComplete="name"
            maxLength="100" required value={form.name}
            onChange={(event) => setForm({ ...form, name: event.target.value })} />
          <label className="form-label" htmlFor="email">Email address</label>
          <input id="email" className="form-control form-control-lg mb-3" type="email"
            autoComplete="email" required value={form.email}
            onChange={(event) => setForm({ ...form, email: event.target.value })} />
          <label className="form-label" htmlFor="password">Password</label>
          <input id="password" className="form-control form-control-lg mb-2" type="password"
            autoComplete="new-password" minLength="6" required value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })} />
          <div className="form-text mb-4">At least 6 characters. Your password is never displayed or stored in the browser.</div>
          <button className="btn btn-dark btn-lg rounded-pill w-100" disabled={submitting}>
            {submitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>
        <p className="text-center mt-4 mb-0">Already have an account? <Link to="/login">Log in</Link></p>
      </section>
    </main>
  );
}
