import api from '@shared/api/httpClient.js';

export const categoryService = {
  getAll: (params = {}) => api.get('/categories', { params }),

  getPage: (params = {}) => api.get('/categories', { params }),

  search: (q, page = 0, size = 12) =>
    api.get('/categories', { params: { q, page, size, sort: 'name,asc' } }),

  create: (payload) => api.post('/categories', payload),

  update: (id, payload) => api.put(`/categories/${id}`, payload),

  uploadImage: (id, formData) =>
    api.post(`/categories/${id}/image`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  delete: (id) => api.delete(`/categories/${id}`),
};
