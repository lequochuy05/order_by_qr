import api from '@shared/api/httpClient.js';

export const categoryService = {
  getPage: (params = {}) => api.get('/categories', { params }),

  create: (payload) => api.post('/categories', payload),

  update: (id, payload) => api.put(`/categories/${id}`, payload),

  uploadImage: (id, formData) => api.post(`/categories/${id}/image`, formData),

  delete: (id) => api.delete(`/categories/${id}`),
};
