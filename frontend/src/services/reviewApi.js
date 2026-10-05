import api from './api';

export const reviewApi = {
  all: (params) => api.get('/reviews', { params }),
  create: (payload) => api.post('/reviews', payload),
  get: (id) => api.get(`/reviews/${id}`),
  forProperty: (propertyId, params) => api.get(`/reviews/property/${propertyId}`, { params }),
  mine: () => api.get('/reviews/my'),
  update: (id, payload) => api.put(`/reviews/${id}`, payload),
  remove: (id) => api.delete(`/reviews/${id}`),
};
