import { useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage } from '../services/api';
import { authApi } from '../services/authApi';

export default function Profile() {
  const { user, refreshUser } = useAuth();
  const [form, setForm] = useState({ name: user?.name || '', email: user?.email || '' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => setForm({ name: user?.name || '', email: user?.email || '' }), [user]);

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      await authApi.updateProfile(form);
      await refreshUser();
      toast.success('Profile updated.');
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Profile could not be updated.'));
    } finally {
      setSaving(false);
    }
  }

  return <main className="container dashboard-page">
    <div className="dashboard-intro"><p className="eyebrow">YOUR ACCOUNT</p><h1>Profile <em>details.</em></h1></div>
    <section className="profile-panel">
      <div className="profile-avatar">{(user?.name || 'S').slice(0, 1).toUpperCase()}</div>
      <div><h2>{user?.name}</h2><p>{(user?.roles || []).join(' · ')}</p></div>
      <form onSubmit={submit} className="profile-form">
        {error && <div className="alert alert-danger" role="alert">{error}</div>}
        <label className="form-label">Name<input className="form-control" maxLength="100" required value={form.name}
          onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
        <label className="form-label">Email<input className="form-control" type="email" required value={form.email}
          onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
        <button className="btn btn-dark rounded-pill px-4 mt-2" disabled={saving}>{saving ? 'Saving…' : 'Save profile'}</button>
      </form>
    </section>
  </main>;
}
