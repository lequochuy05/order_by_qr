import api from '@shared/api/httpClient.js';
import { createCachedRequest } from '@shared/lib/cacheUtils.js';

const { requestFn: getSettings } = createCachedRequest(() => api.get('/settings'), 0);
const { requestFn: getPublicSettings } = createCachedRequest(() => api.get('/public/settings'), 0);

export const settingsService = {
  get: getSettings,
  getPublic: getPublicSettings,

  update: async (data) => {
    return await api.put('/settings', data);
  },

  uploadLogo: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return await api.post('/settings/logo', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};
