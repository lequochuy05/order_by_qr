import api from '@shared/api/httpClient.js';
import { createKeyedCachedRequest, stableStringify } from '@shared/lib/cacheUtils.js';

// In-flight deduplication only (cacheMs = 0); React Query owns caching & WebSocket invalidation.
const { requestFn: getUserPageDeduped } = createKeyedCachedRequest(
  (requestParams) => api.get('/users', { params: requestParams }),
  0,
  stableStringify,
);

export const userService = {
  getPage: (params = {}, { force = false } = {}) => {
    const page = Number(params.page || 0);
    const size = Number(params.size || 24);
    return getUserPageDeduped({ page, size, sort: 'fullName,asc', ...params }, { force });
  },
  create: (data) => api.post('/users', data),
  update: (id, data) => api.put(`/users/${id}`, data),
  uploadAvatar: (id, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`/users/${id}/avatar`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  delete: (id) => api.delete(`/users/${id}`),
};
