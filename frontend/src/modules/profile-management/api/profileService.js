import api from '@shared/api/httpClient.js';

export const profileService = {
  getMe: async () => {
    return await api.get('/users/me');
  },

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
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  }
};
