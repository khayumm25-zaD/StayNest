import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { notificationApi } from '../services/notificationApi';

export default function NotificationBell() {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');

  async function refresh() {
    try {
      const { data } = await notificationApi.mine({ page: 0, size: 5 });
      setNotifications(data.content || []);
      setUnreadCount(data.unreadCount || 0);
      setError('');
    } catch {
      setError('Notifications are temporarily unavailable.');
    }
  }

  useEffect(() => { refresh(); }, []);

  async function markRead(id) {
    try {
      await notificationApi.markRead(id);
      await refresh();
    } catch {
      setError('Could not mark this notification as read.');
    }
  }

  return (
    <div className="notification-menu">
      <button className="btn btn-light rounded-circle notification-trigger" aria-label={`Notifications, ${unreadCount} unread`}
        aria-expanded={open} onClick={() => { setOpen(!open); if (!open) refresh(); }}>
        <span aria-hidden="true">♧</span>
        {unreadCount > 0 && <span className="notification-count">{unreadCount > 9 ? '9+' : unreadCount}</span>}
      </button>
      {open && <div className="notification-popover">
        <div className="d-flex justify-content-between align-items-center mb-3">
          <strong>Notifications</strong><span className="small text-muted">{unreadCount} unread</span>
        </div>
        {error && <p className="small text-danger">{error}</p>}
        {!notifications.length && !error && <p className="small text-muted mb-3">You’re all caught up.</p>}
        {notifications.map((item) => <button key={item.id} className={`notification-item ${item.unread ? 'is-unread' : ''}`}
          onClick={() => item.unread && markRead(item.id)}>
          <span className="notification-dot" />
          <span><span className="d-block fw-semibold">{item.type.replaceAll('_', ' ')}</span>
            <span className="small text-muted">{item.message}</span></span>
        </button>)}
        <Link className="btn btn-outline-dark btn-sm rounded-pill w-100 mt-2" to="/notifications" onClick={() => setOpen(false)}>
          View all notifications
        </Link>
      </div>}
    </div>
  );
}
