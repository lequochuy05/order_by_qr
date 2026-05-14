import api from './api';
import { refreshAccessToken } from './api';

export const authService = {
  login: async (email, password) => {
    return await api.post('/users/login', { email, password });
  },

  refresh: async () => {
    return await refreshAccessToken();
  },

  logout: async () => {
    return await api.post('/users/logout');
  },

  getProfile: async () => {
    return await api.get('/auth/profile');
  },
};
