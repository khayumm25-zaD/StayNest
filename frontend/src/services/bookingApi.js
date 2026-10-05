import api from './api';

export const bookingApi = {
  availability: (params) => api.get('/bookings/availability', { params }),
  create: (payload) => api.post('/bookings', payload),
  get: (id) => api.get(`/bookings/${id}`),
  mine: () => api.get('/bookings/my'),
  host: () => api.get('/bookings/host'),
  forProperty: (propertyId) => api.get(`/bookings/property/${propertyId}`),
  cancel: (id) => api.put(`/bookings/${id}/cancel`),
  updateStatus: (id, status) => api.put(`/bookings/${id}/status`, { status }),
};
