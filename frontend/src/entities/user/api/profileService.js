import api from '@shared/api/httpClient.js';
import { createCachedRequest } from '@shared/lib/cacheUtils.js';

const { requestFn: getMe, clearCache: clearProfileCache } = createCachedRequest(
  () => api.get('/users/me'),
  0,
);

export const profileService = {
  clearCache: clearProfileCache,
  getMe,

  updateMe: async (data) => {
    clearProfileCache();
    const res = await api.patch('/users/me', data);
    clearProfileCache();
    return res;
  },

  changePassword: async (data) => {
    clearProfileCache();
    const res = await api.patch('/users/me/password', data);
    clearProfileCache();
    return res;
  },

  uploadAvatar: async (file) => {
    clearProfileCache();
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post('/users/me/avatar', formData);
    clearProfileCache();
    return res;
  },
};
