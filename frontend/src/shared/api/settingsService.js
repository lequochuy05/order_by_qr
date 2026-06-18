import api from '@shared/api/httpClient.js';
import { createCachedRequest } from '@shared/lib/cacheUtils.js';

const { requestFn: getSettings } = createCachedRequest(() => api.get('/settings'), 0);

export const settingsService = {
  get: getSettings,

  update: async (data) => {
    return await api.put('/settings', data);
  },
};
