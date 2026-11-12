import api from '@shared/api/httpClient.js';
import { createCachedRequest } from '@shared/lib/cacheUtils.js';

const SETTINGS_CACHE_MS = 60_000;
const PUBLIC_SETTINGS_CACHE_MS = 5 * 60_000;

const { requestFn: getSettings, clearCache: clearSettingsCache } = createCachedRequest(
  () => api.get('/settings'),
  SETTINGS_CACHE_MS,
);
const { requestFn: getPublicSettings, clearCache: clearPublicSettingsCache } =
  createCachedRequest(() => api.get('/public/settings'), PUBLIC_SETTINGS_CACHE_MS);

const clearSettingsCaches = () => {
  clearSettingsCache();
  clearPublicSettingsCache();
};

export const settingsService = {
  get: getSettings,
  getPublic: getPublicSettings,

  update: async (data) => {
    const response = await api.put('/settings', data);
    clearSettingsCaches();
    return response;
  },

  uploadLogo: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/settings/logo', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    clearSettingsCaches();
    return response;
  },

  clearCache: clearSettingsCaches,
};
