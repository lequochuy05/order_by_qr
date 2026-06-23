import api, { refreshAccessToken } from '@shared/api/httpClient.js';

export const authService = {
  login: async (email, password) => {
    return await api.post('/auth/login', { email, password });
  },

  refresh: async () => {
    return await refreshAccessToken();
  },

  logout: async () => {
    return await api.post('/auth/logout');
  },

  getProfile: async () => {
    return await api.get('/users/me');
  },

  forgotPassword: async (email) => {
    return await api.post('/auth/forgot-password-email', null, {
      params: { email },
      skipAuth: true,
    });
  },

  resetPassword: async (token, newPassword) => {
    return await api.post(
      '/auth/reset-password-email',
      { token, newPassword },
      {
        skipAuth: true,
      },
    );
  },
};
