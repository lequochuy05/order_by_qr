import api from '@shared/api/httpClient.js';
import { createCachedRequest } from '@shared/lib/cacheUtils.js';

const { requestFn: getMe } = createCachedRequest(() => api.get('/users/me'), 0);

export const profileService = {
  getMe: getMe,

  updateMe: async (data) => {
    return await api.patch('/users/me', data);
  },

  changePassword: async (data) => {
    return await api.patch('/users/me/password', data);
  },

  uploadAvatar: async (file) => {
    const formData = new FormData();
    formData.append('file', file);

    return await api.post('/users/me/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};
