import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import api, { authStorage } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => authStorage.getUser());
  const [loading, setLoading] = useState(Boolean(authStorage.getToken()));

  const clearSession = useCallback(() => {
    authStorage.clear();
    setUser(null);
    setLoading(false);
  }, []);

  const loadCurrentUser = useCallback(async () => {
    if (!authStorage.getToken()) {
      setUser(null);
      setLoading(false);
      return null;
    }
    setLoading(true);
    try {
      const response = await api.get('/auth/me');
      setUser(response.data);
      authStorage.setUser(response.data);
      return response.data;
    } catch (error) {
      if (error.response?.status === 401) clearSession();
      return null;
    } finally {
      setLoading(false);
    }
  }, [clearSession]);

  useEffect(() => {
    loadCurrentUser();
    const onUnauthorized = () => clearSession();
    window.addEventListener('staynest:unauthorized', onUnauthorized);
    return () => window.removeEventListener('staynest:unauthorized', onUnauthorized);
  }, [clearSession, loadCurrentUser]);

  const acceptAuthResponse = useCallback(async (data) => {
    authStorage.setToken(data.token);
    const currentUser = await loadCurrentUser();
    if (currentUser) return currentUser;
    const fallbackUser = { name: data.name, email: data.email, roles: data.roles || [] };
    setUser(fallbackUser);
    authStorage.setUser(fallbackUser);
    return fallbackUser;
  }, [loadCurrentUser]);

  const login = useCallback(async (credentials) => {
    const { data } = await api.post('/auth/login', credentials);
    return acceptAuthResponse(data);
  }, [acceptAuthResponse]);

  const register = useCallback(async (registration) => {
    const { data } = await api.post('/auth/register', registration);
    return acceptAuthResponse(data);
  }, [acceptAuthResponse]);

  const logout = useCallback(async () => {
    try {
      if (authStorage.getToken()) await api.post('/auth/logout');
    } finally {
      clearSession();
    }
  }, [clearSession]);

  const value = useMemo(() => ({
    user,
    loading,
    isAuthenticated: Boolean(user),
    hasRole: (role) => (user?.roles || []).some((userRole) => String(userRole).replace('ROLE_', '') === role),
    login,
    register,
    logout,
    refreshUser: loadCurrentUser,
  }), [user, loading, login, register, logout, loadCurrentUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
