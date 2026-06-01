import api from '../api';

export const settingsService = {
  get: async () => {
    return await api.get('/settings');
  },

  update: async (data) => {
    return await api.put('/settings', data);
  }
};
