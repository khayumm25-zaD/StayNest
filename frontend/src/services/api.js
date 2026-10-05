import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
const TOKEN_KEY = 'staynest.accessToken';
const USER_KEY = 'staynest.user';

export const authStorage = {
  getToken: () => window.localStorage.getItem(TOKEN_KEY),
  setToken: (token) => window.localStorage.setItem(TOKEN_KEY, token),
  getUser: () => {
    try {
      return JSON.parse(window.localStorage.getItem(USER_KEY) || 'null');
    } catch {
      window.localStorage.removeItem(USER_KEY);
      return null;
    }
  },
  setUser: (user) => window.localStorage.setItem(USER_KEY, JSON.stringify(user)),
  clear: () => {
    window.localStorage.removeItem(TOKEN_KEY);
    window.localStorage.removeItem(USER_KEY);
  },
};

const api = axios.create({
  baseURL: API_BASE_URL.replace(/\/$/, ''),
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const token = authStorage.getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      authStorage.clear();
      window.dispatchEvent(new Event('staynest:unauthorized'));
    }
    return Promise.reject(error);
  },
);

export function getApiErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (!error?.response) return 'Unable to connect. Check your network and try again.';
  const data = error.response.data;
  return data?.message || data?.error || fallback;
}

export const apiBaseUrl = API_BASE_URL;
export default api;
