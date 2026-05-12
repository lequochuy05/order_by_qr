import api from './api';

export const authService = {
  login: async (email, password) => {
    return await api.post('/users/login', { email, password });
  },

  getProfile: async () => {
    return await api.get('/auth/profile');
  },
};