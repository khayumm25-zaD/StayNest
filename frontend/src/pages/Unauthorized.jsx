import { Link } from 'react-router-dom';

export default function Unauthorized() {
  return <main className="container error-page"><p className="eyebrow">NOT YOUR NEIGHBOURHOOD</p>
    <h1>That page is <em>off limits.</em></h1><p>Your account doesn’t have permission to view it.</p>
    <Link to="/" className="btn btn-dark rounded-pill px-4">Back to StayNest</Link></main>;
}
