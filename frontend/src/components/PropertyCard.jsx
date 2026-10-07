import { Link } from 'react-router-dom';

const propertyImages = [
  '/property-images/Hyderabad.jpg',
  '/property-images/Alleppey.jpg',
  '/property-images/Goa.jpg',
  '/property-images/Bangalore.jpg',
  '/property-images/chennai.jpg',
  '/property-images/Delhi.jpg',
];

export default function PropertyCard({ property, index = 0 }) {
  const location = [property.city, property.state, property.country]
    .filter(Boolean)
    .join(', ');

  const imageUrl = propertyImages[index % propertyImages.length];

  return (
    <article className="property-card">
      <Link
        className="property-photo"
        style={{
          backgroundImage: `url("${imageUrl}")`,
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
        to={`/properties/${property.id}`}
        aria-label={`View ${property.title}`}
      >
        <span className="photo-label">
          {property.propertyType || 'A place to stay'}
        </span>
      </Link>

      <div className="property-card-body">
        <div className="d-flex justify-content-between gap-2 align-items-start">
          <div>
            <p className="property-location">
              {location || 'A wonderful destination'}
            </p>

            <h3>
              <Link to={`/properties/${property.id}`}>
                {property.title}
              </Link>
            </h3>
          </div>

          <span
            className="property-rating"
            aria-label="New property rating"
          >
            ★ <span>New</span>
          </span>
        </div>

        <p className="property-meta">
          {property.maxGuests || 1} guests · {property.bedrooms ?? 0} bedrooms
        </p>

        <p className="property-price">
          <strong>
            ₹{Number(property.pricePerNight || 0).toLocaleString('en-IN')}
          </strong>
          <span> / night</span>
        </p>
      </div>
    </article>
  );
}