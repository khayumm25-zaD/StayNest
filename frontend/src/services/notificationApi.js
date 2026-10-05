import api from './api';

export const notificationApi = {
  all: (params = {}) => api.get('/notifications', { params }),
  mine: (params = {}) => api.get('/notifications/my', { params }),
  get: (id) => api.get(`/notifications/${id}`),
  create: (payload) => api.post('/notifications', payload),
  markRead: (id) => api.put(`/notifications/${id}/read`),
  markAllRead: () => api.put('/notifications/read-all'),
};
