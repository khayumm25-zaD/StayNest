import api from './api';

export const paymentApi = {
  create: (payload) => api.post('/payments', payload),
  get: (id) => api.get(`/payments/${id}`),
  forBooking: (bookingId) => api.get(`/payments/booking/${bookingId}`),
  mine: () => api.get('/payments/my'),
  refund: (id) => api.post(`/payments/${id}/refund`),
};
