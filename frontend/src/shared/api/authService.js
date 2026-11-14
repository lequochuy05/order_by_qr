import api, { refreshAccessToken } from './httpClient.js';

export const authService = {
  login: async (email, password) => api.post('/auth/login', { email, password }),

  refresh: async () => refreshAccessToken(),

  logout: async () => api.post('/auth/logout'),

  getProfile: async () => api.get('/users/me'),

  forgotPassword: async (email) =>
    api.post('/auth/forgot-password-email', null, {
      params: { email },
      skipAuth: true,
    }),

  verify2fa: async (preAuthToken, otp) =>
    api.post('/auth/verify-2fa', { preAuthToken, otp }, { skipAuth: true }),

  resend2fa: async (preAuthToken) =>
    api.post('/auth/resend-2fa', { preAuthToken }, { skipAuth: true }),

  resetPassword: async (token, newPassword) =>
    api.post(
      '/auth/reset-password-email',
      { token, newPassword },
      {
        skipAuth: true,
      },
    ),
};
