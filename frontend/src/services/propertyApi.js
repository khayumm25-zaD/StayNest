import api from './api';

export const propertyApi = {
  list: () => api.get('/properties'),
  get: (id) => api.get(`/properties/${id}`),
  search: (params) => api.get('/properties/search', { params }),
  create: (payload) => api.post('/properties', payload),
  update: (id, payload) => api.put(`/properties/${id}`, payload),
  remove: (id) => api.delete(`/properties/${id}`),
};
