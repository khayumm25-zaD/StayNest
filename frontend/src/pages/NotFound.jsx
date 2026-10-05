import { Link } from 'react-router-dom';

export default function NotFound() {
  return <main className="container error-page"><p className="eyebrow">404 · LOST YOUR WAY?</p>
    <h1>This page wandered <em>off.</em></h1><p>Let’s get you back to somewhere lovely.</p>
    <Link to="/properties" className="btn btn-dark rounded-pill px-4">Explore stays</Link></main>;
}
