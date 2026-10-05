import api from './api';

export const authApi = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (registration) => api.post('/auth/register', registration),
  logout: () => api.post('/auth/logout'),
  me: () => api.get('/auth/me'),
  updateProfile: (profile) => api.put('/users/me', profile),
  users: () => api.get('/users'),
  updateRoles: (id, roles) => api.patch(`/users/${id}/roles`, roles),
};
