import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import NotificationBell from './NotificationBell';

export default function Navbar() {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate('/');
  }

  const dashboardPath = hasRole('ADMIN') ? '/admin' : hasRole('HOST') ? '/host' : '/dashboard';

  return (
    <nav className="navbar navbar-expand-lg staynest-navbar sticky-top">
      <div className="container">
        <Link className="navbar-brand staynest-brand" to="/">StayNest<span>.</span></Link>
        <button className="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#staynestNav" aria-label="Toggle navigation">
          <span className="navbar-toggler-icon" />
        </button>
        <div className="collapse navbar-collapse" id="staynestNav">
          <div className="navbar-nav mx-auto gap-lg-2">
            <NavLink className="nav-link" to="/properties">Find a stay</NavLink>
            <a className="nav-link" href="/#experiences">Experiences</a>
            <a className="nav-link" href="/#about">About</a>
          </div>
          <div className="d-flex align-items-center gap-2">
            {user ? <>
              <NotificationBell />
              <Link className="btn btn-link text-dark text-decoration-none" to={dashboardPath}>{user.name || 'Account'}</Link>
              <button className="btn btn-outline-dark rounded-pill px-3" onClick={handleLogout}>Sign out</button>
            </> : <>
              <Link className="btn btn-link text-dark text-decoration-none" to="/login">Log in</Link>
              <Link className="btn btn-dark rounded-pill px-4" to="/register">Get started</Link>
            </>}
          </div>
        </div>
      </div>
    </nav>
  );
}
