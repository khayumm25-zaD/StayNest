import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage } from '../services/api';

export default function Login() {
  const [form, setForm] = useState({ email: '', password: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  async function submit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const user = await login(form);
      toast.success('Welcome back!');
      const defaultRoute = user.roles?.includes('ADMIN') ? '/admin'
        : user.roles?.includes('HOST') ? '/host' : '/dashboard';
      navigate(location.state?.from?.pathname || defaultRoute, { replace: true });
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'We could not sign you in with those details.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-shell">
      <section className="auth-card">
        <Link className="staynest-brand mb-4 d-inline-block" to="/">StayNest<span>.</span></Link>
        <p className="eyebrow">WELCOME BACK</p>
        <h1 className="h2 fw-bold mb-2">Log in to your account</h1>
        <p className="text-muted mb-4">Your next memorable stay is just around the corner.</p>
        {error && <div className="alert alert-danger" role="alert">{error}</div>}
        <form onSubmit={submit}>
          <label className="form-label" htmlFor="email">Email address</label>
          <input id="email" className="form-control form-control-lg mb-3" type="email"
            autoComplete="email" required value={form.email}
            onChange={(event) => setForm({ ...form, email: event.target.value })} />
          <label className="form-label" htmlFor="password">Password</label>
          <input id="password" className="form-control form-control-lg mb-4" type="password"
            autoComplete="current-password" required value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })} />
          <button className="btn btn-dark btn-lg rounded-pill w-100" disabled={submitting}>
            {submitting ? 'Signing in…' : 'Log in'}
          </button>
        </form>
        <p className="text-center mt-4 mb-0">New to StayNest? <Link to="/register">Create an account</Link></p>
      </section>
    </main>
  );
}
