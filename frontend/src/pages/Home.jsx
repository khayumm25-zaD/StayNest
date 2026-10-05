import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PropertyCard from '../components/PropertyCard';
import { propertyApi } from '../services/propertyApi';

const destinations = [
  { name: 'Goa', note: 'Coastal slow living', icon: '☼', tone: 'destination-sand' },
  { name: 'Jaipur', note: 'Colourful old-city stays', icon: '✺', tone: 'destination-rose' },
  { name: 'Manali', note: 'Cabins in the mountains', icon: '⌁', tone: 'destination-pine' },
  { name: 'Kerala', note: 'Backwaters & greenery', icon: '〰', tone: 'destination-water' },
];

export default function Home() {
  const [featured, setFeatured] = useState([]);
  const [featuredError, setFeaturedError] = useState('');
  const [location, setLocation] = useState('');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    propertyApi.list()
      .then(({ data }) => setFeatured((Array.isArray(data) ? data : []).slice(0, 3)))
      .catch(() => {
        setFeatured([]);
        setFeaturedError('Featured stays are temporarily unavailable.');
      })
      .finally(() => setLoading(false));
  }, []);

  function search(event) {
    event.preventDefault();
    navigate(`/properties${location.trim() ? `?location=${encodeURIComponent(location.trim())}` : ''}`);
  }

  return (
    <>
      <section className="home-hero">
        <div className="container home-hero-grid">
          <div className="home-hero-copy">
            <p className="eyebrow">STAY A LITTLE CLOSER</p>
            <h1>Find your kind<br />of <em>away.</em></h1>
            <p className="hero-description">Thoughtful homestays, local hosts, and the little moments that make a trip yours.</p>
            <form className="hero-search" onSubmit={search}>
              <label className="search-input-wrap">
                <span className="search-mark" aria-hidden="true">⌕</span>
                <span><small>WHERE TO?</small>
                  <input aria-label="Destination" placeholder="Try Goa, Jaipur, Manali…" value={location}
                    onChange={(event) => setLocation(event.target.value)} /></span>
              </label>
              <button className="btn btn-dark rounded-pill" type="submit">Explore stays <span aria-hidden="true">→</span></button>
            </form>
            <div className="hero-proof"><span className="proof-avatars">S&nbsp; M&nbsp; A</span><span>Loved by travellers looking for more than a room.</span></div>
          </div>
          <div className="hero-art" aria-label="Illustration of a sunny coastal homestay">
            <div className="hero-sun" />
            <div className="hero-cloud cloud-one" />
            <div className="hero-cloud cloud-two" />
            <div className="hero-hill hill-back" />
            <div className="hero-hill hill-front" />
            <div className="hero-house"><div className="house-roof" /><div className="house-body"><i /><i /></div></div>
            <div className="hero-art-caption"><span>THE SLOW HOUSE</span><strong>Somewhere by the sea</strong></div>
            <div className="art-stamp">A stay<br />to remember</div>
          </div>
        </div>
        <div className="hero-bottom container"><span>MADE FOR YOUR NEXT GOOD STORY</span><span>01 — FIND · 02 — FEEL · 03 — REMEMBER</span></div>
      </section>

      <section className="section-space container" id="destinations">
        <div className="section-heading">
          <div><p className="eyebrow">A GOOD PLACE TO BEGIN</p><h2>Where are you <em>drawn to?</em></h2></div>
          <Link to="/properties" className="text-link">All destinations <span>↗</span></Link>
        </div>
        <div className="destination-grid">
          {destinations.map((destination) => <button type="button" key={destination.name}
            className={`destination-card ${destination.tone}`} onClick={() => navigate(`/properties?location=${destination.name}`)}>
            <span className="destination-icon">{destination.icon}</span>
            <span className="destination-name">{destination.name}</span>
            <span className="destination-note">{destination.note}</span>
            <span className="destination-arrow" aria-hidden="true">↗</span>
          </button>)}
        </div>
      </section>

      <section className="section-space featured-section">
        <div className="container">
          <div className="section-heading">
            <div><p className="eyebrow">PLACES WITH A LITTLE EXTRA</p><h2>Stays worth <em>staying in.</em></h2></div>
            <Link to="/properties" className="text-link">See all stays <span>↗</span></Link>
          </div>
          {loading ? <div className="loading-row" role="status">Finding lovely places…</div>
            : featured.length ? <div className="row g-4">{featured.map((place, index) =>
              <div className="col-md-6 col-lg-4" key={place.id}><PropertyCard property={place} index={index} /></div>)}</div>
              : <div className="empty-panel"><span>⌂</span><h3>{featuredError ? 'We couldn’t load featured stays.' : 'Your next stay is taking shape.'}</h3><p>{featuredError || 'New homes will appear here as hosts join StayNest.'}</p><Link to="/properties" className="btn btn-dark rounded-pill">Explore all stays</Link></div>}
        </div>
      </section>

      <section className="section-space container">
        <div className="section-heading"><div><p className="eyebrow">YOUR KIND OF PLACE</p><h2>Stay your <em>own way.</em></h2></div>
          <Link to="/properties" className="text-link">Browse all stays <span>↗</span></Link></div>
        <div className="category-row">
          {[['Homestays', 'Homestay', 'Places that feel lived in'], ['Villas', 'Villa', 'A little space to yourself'], ['Cabins', 'Cabin', 'Closer to the quiet'], ['Farm stays', 'Farm stay', 'Slow down somewhere green']].map(([name, type, note], index) =>
            <Link className={`category-tile category-${index}`} key={name} to={`/properties?propertyType=${encodeURIComponent(type)}`}>
              <span className="category-number">0{index + 1}</span><strong>{name}</strong><small>{note}</small><span className="category-arrow">↗</span>
            </Link>)}
        </div>
      </section>

      <section className="section-space container" id="experiences">
        <div className="experience-banner">
          <div><p className="eyebrow">NOT JUST A PLACE TO SLEEP</p><h2>Make room for<br /><em>the unexpected.</em></h2>
            <p>Follow the smell of fresh chai. Take the road with no sign. Let your host show you the rest.</p>
            <Link to="/properties" className="btn btn-light rounded-pill px-4">Find your experience <span>→</span></Link></div>
          <div className="experience-orbit" aria-hidden="true"><span>LOCAL</span><b>✳</b><span>UNSCRIPTED</span><b>✳</b><span>YOURS</span></div>
        </div>
      </section>

      <section className="section-space why-section" id="about">
        <div className="container why-grid">
          <div><p className="eyebrow">A BETTER WAY TO GET AWAY</p><h2>Good stays.<br /><em>Good people.</em><br />Good stories.</h2></div>
          <div className="why-points">
            <article><span>01</span><div><h3>Stay with a story</h3><p>Handpicked homes with character, cared for by the people who know them best.</p></div></article>
            <article><span>02</span><div><h3>Feel at home, anywhere</h3><p>More breathing room, more local know-how, and space to travel at your own pace.</p></div></article>
            <article><span>03</span><div><h3>Book with confidence</h3><p>Clear prices, secure accounts, and a simple booking journey from browse to arrival.</p></div></article>
          </div>
        </div>
      </section>

      <section className="section-space container">
        <div className="testimonial-card"><span className="quote-mark">“</span>
          <blockquote>The best part wasn’t the view. It was being welcomed like we’d been coming back for years.</blockquote>
          <div className="testimonial-byline"><span className="testimonial-avatar">R</span><span><strong>Riya & Arjun</strong><small>Guests in the hills</small></span></div>
        </div>
      </section>

      <section className="last-call"><div className="container"><div><p className="eyebrow">YOUR NEXT STORY STARTS SOMEWHERE</p>
        <h2>Go where you <em>feel good.</em></h2></div><Link to="/properties" className="btn btn-light rounded-pill px-4 py-3">Find your StayNest <span>↗</span></Link></div></section>
      <footer className="site-footer"><div className="container footer-content"><Link className="staynest-brand" to="/">StayNest<span>.</span></Link>
        <span>Thoughtful stays for the way you want to travel.</span><span>© {new Date().getFullYear()} StayNest</span></div></footer>
    </>
  );
}
