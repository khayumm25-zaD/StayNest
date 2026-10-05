import { Link } from 'react-router-dom';

const palettes = [
  'photo-coast', 'photo-cabin', 'photo-city', 'photo-villa',
];

export default function PropertyCard({ property, index = 0 }) {
  const location = [property.city, property.state, property.country].filter(Boolean).join(', ');
  return (
    <article className="property-card">
      <Link className={`property-photo ${palettes[index % palettes.length]}`} to={`/properties/${property.id}`}
        aria-label={`View ${property.title}`}>
        <span className="photo-label">{property.propertyType || 'A place to stay'}</span>
        <span className="photo-illustration" aria-hidden="true">{['⌂', '⌁', '◒', '⌂'][index % 4]}</span>
      </Link>
      <div className="property-card-body">
        <div className="d-flex justify-content-between gap-2 align-items-start">
          <div><p className="property-location">{location || 'A wonderful destination'}</p>
            <h3><Link to={`/properties/${property.id}`}>{property.title}</Link></h3></div>
          <span className="property-rating" aria-label="New property rating">★ <span>New</span></span>
        </div>
        <p className="property-meta">{property.maxGuests || 1} guests · {property.bedrooms ?? 0} bedrooms</p>
        <p className="property-price"><strong>₹{Number(property.pricePerNight || 0).toLocaleString('en-IN')}</strong> <span>/ night</span></p>
      </div>
    </article>
  );
}
