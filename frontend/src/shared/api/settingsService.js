import api from './httpClient.js';

export const settingsService = {
  get: async () => {
    return await api.get('/settings');
  },

  update: async (data) => {
    return await api.put('/settings', data);
  }
};
