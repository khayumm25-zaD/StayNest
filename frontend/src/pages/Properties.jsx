import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import PropertyCard from '../components/PropertyCard';
import { getApiErrorMessage } from '../services/api';
import { propertyApi } from '../services/propertyApi';

const initialFilters = { location: '', propertyType: '', minPrice: '', maxPrice: '', guests: '', amenity: '' };

export default function Properties() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [filters, setFilters] = useState({
    ...initialFilters,
    location: searchParams.get('location') || '',
    propertyType: searchParams.get('propertyType') || '',
  });
  const [results, setResults] = useState({ content: [], totalPages: 0, totalElements: 0, number: 0 });
  const [page, setPage] = useState(0);
  const [sortBy, setSortBy] = useState('pricePerNight');
  const [direction, setDirection] = useState('asc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function loadResults(nextPage = page, activeFilters = filters) {
    setLoading(true);
    setError('');
    try {
      const { data } = await propertyApi.search({
        ...Object.fromEntries(Object.entries(activeFilters).filter(([, value]) => value !== '')),
        page: nextPage, size: 9, sortBy, direction,
      });
      setResults(data);
      setPage(nextPage);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'We could not load stays right now.'));
      setResults({ content: [], totalPages: 0, totalElements: 0, number: 0 });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadResults(0, filters); }, []);

  function updateFilter(name, value) {
    setFilters((current) => ({ ...current, [name]: value }));
  }

  function submit(event) {
    event.preventDefault();
    setSearchParams(filters.location ? { location: filters.location } : {});
    loadResults(0, filters);
  }

  return (
    <main className="container listing-page">
      <div className="listing-intro"><p className="eyebrow">FIND YOUR PLACE</p><h1>Somewhere <em>good.</em></h1>
        <p>From slow mornings by the sea to cool nights in the hills, find a stay that feels like yours.</p></div>
      <form className="filter-panel" onSubmit={submit}>
        <div className="filter-grid">
          <label>Destination<input className="form-control" placeholder="City, state or country" value={filters.location} onChange={(e) => updateFilter('location', e.target.value)} /></label>
          <label>Stay type<input className="form-control" placeholder="Villa, cabin…" value={filters.propertyType} onChange={(e) => updateFilter('propertyType', e.target.value)} /></label>
          <label>Min price<input className="form-control" type="number" min="0" placeholder="₹ 0" value={filters.minPrice} onChange={(e) => updateFilter('minPrice', e.target.value)} /></label>
          <label>Max price<input className="form-control" type="number" min="0" placeholder="No limit" value={filters.maxPrice} onChange={(e) => updateFilter('maxPrice', e.target.value)} /></label>
          <label>Guests<input className="form-control" type="number" min="1" placeholder="Any" value={filters.guests} onChange={(e) => updateFilter('guests', e.target.value)} /></label>
          <label>Amenity<input className="form-control" placeholder="Wi-Fi, pool…" value={filters.amenity} onChange={(e) => updateFilter('amenity', e.target.value)} /></label>
        </div>
        <div className="filter-actions"><div className="d-flex gap-2 align-items-center"><label className="small text-muted" htmlFor="sortBy">Sort by</label>
          <select id="sortBy" className="form-select form-select-sm" value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
            <option value="pricePerNight">Price</option><option value="title">Name</option><option value="city">Location</option>
          </select>
          <select aria-label="Sort direction" className="form-select form-select-sm" value={direction} onChange={(e) => setDirection(e.target.value)}>
            <option value="asc">Low to high</option><option value="desc">High to low</option>
          </select></div>
          <div className="d-flex gap-2"><button type="button" className="btn btn-link text-dark" onClick={() => {
            setFilters(initialFilters); setSearchParams({}); loadResults(0, initialFilters);
          }}>Clear filters</button><button type="submit" className="btn btn-dark rounded-pill px-4">Search stays</button></div></div>
      </form>
      <div className="results-heading"><div><p className="eyebrow mb-1">THE STAY COLLECTION</p>
        <h2>{loading ? 'Looking for your stay…' : `${results.totalElements} ${results.totalElements === 1 ? 'place' : 'places'} to discover`}</h2></div></div>
      {error && <div className="alert alert-danger" role="alert">{error} <button className="btn btn-sm btn-outline-danger ms-2" onClick={() => loadResults()}>Try again</button></div>}
      {loading ? <div className="loading-row" role="status">Finding your next favourite place…</div>
        : results.content?.length ? <div className="row g-4">{results.content.map((property, index) =>
          <div className="col-md-6 col-lg-4" key={property.id}><PropertyCard property={property} index={index + page * 3} /></div>)}</div>
          : !error && <div className="empty-panel"><span>⌕</span><h3>No stays match just yet.</h3><p>Try a nearby place or loosen a filter to see more options.</p></div>}
      {results.totalPages > 1 && <nav className="pagination-wrap" aria-label="Property pages">
        <button className="btn btn-outline-dark rounded-pill" disabled={page <= 0 || loading} onClick={() => loadResults(page - 1)}>← Previous</button>
        <span>Page {page + 1} of {results.totalPages}</span>
        <button className="btn btn-outline-dark rounded-pill" disabled={page + 1 >= results.totalPages || loading} onClick={() => loadResults(page + 1)}>Next →</button>
      </nav>}
    </main>
  );
}
