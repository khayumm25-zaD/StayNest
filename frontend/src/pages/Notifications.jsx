import { useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import { notificationApi } from '../services/notificationApi';
import { getApiErrorMessage } from '../services/api';

export default function Notifications() {
  const [page, setPage] = useState(null);
  const [pageNumber, setPageNumber] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function load(target = pageNumber) {
    setLoading(true);
    try {
      const { data } = await notificationApi.mine({ page: target, size: 10 });
      setPage(data);
      setPageNumber(target);
      setError('');
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Notifications could not be loaded.'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(0); }, []);

  async function markRead(id) {
    try {
      await notificationApi.markRead(id);
      await load();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Could not update notification.'));
    }
  }

  async function markAllRead() {
    try {
      await notificationApi.markAllRead();
      toast.success('All notifications marked as read.');
      await load();
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Could not update notifications.'));
    }
  }

  return <main className="container dashboard-page">
    <div className="dashboard-heading-row"><div><p className="eyebrow">YOUR INBOX</p><h1>Little <em>updates.</em></h1></div>
      {!!page?.unreadCount && <button className="btn btn-outline-dark rounded-pill" onClick={markAllRead}>Mark all read</button>}</div>
    {error && <div className="alert alert-danger" role="alert">{error}</div>}
    {loading ? <div className="loading-row">Loading your updates…</div>
      : page?.content?.length ? <div className="notification-list">{page.content.map((item) =>
        <article key={item.id} className={`notification-full ${item.unread ? 'is-unread' : ''}`}>
          <span className="notification-type-icon">{item.type.startsWith('PAYMENT') ? '₹' : item.type.startsWith('REVIEW') ? '★' : '⌂'}</span>
          <div className="flex-grow-1"><div className="d-flex justify-content-between gap-3"><strong>{item.type.replaceAll('_', ' ')}</strong>
            <time className="small text-muted">{new Date(item.createdAt).toLocaleString()}</time></div>
            <p className="mb-0 mt-1">{item.message}</p>{item.readAt && <small className="text-muted">Read {new Date(item.readAt).toLocaleString()}</small>}</div>
          {item.unread && <button className="btn btn-sm btn-outline-dark rounded-pill" onClick={() => markRead(item.id)}>Mark read</button>}
        </article>)}
        <div className="pagination-wrap"><button className="btn btn-outline-dark rounded-pill" disabled={pageNumber === 0} onClick={() => load(pageNumber - 1)}>← Previous</button>
          <span>Page {pageNumber + 1} of {Math.max(1, page.totalPages)}</span>
          <button className="btn btn-outline-dark rounded-pill" disabled={pageNumber + 1 >= page.totalPages} onClick={() => load(pageNumber + 1)}>Next →</button></div>
      </div> : <div className="empty-panel"><span>♧</span><h2>No updates yet.</h2><p>Important booking and trip updates will show up here.</p></div>}
  </main>;
}
