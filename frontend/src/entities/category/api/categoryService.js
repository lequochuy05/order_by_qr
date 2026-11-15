import api from '@shared/api/httpClient.js';

export const categoryService = {
  getPage: (params = {}) => api.get('/categories', { params }),

  // getById fix: needed so CategoryManagement can load a fresh version before edit modal opens.
  getById: (id) => api.get(`/categories/${id}`),

  create: (payload) => api.post('/categories', payload),

  update: (id, payload) => api.put(`/categories/${id}`, payload),

  // M-fix: version required by backend uploadImage endpoint for optimistic locking.
  uploadImage: (id, version, formData) =>
    api.post(`/categories/${id}/image`, formData, { params: { version } }),

  // L-fix: version is now required by the backend DELETE endpoint for optimistic locking.
  delete: (id, version) => api.delete(`/categories/${id}`, { params: { version } }),
};
