import api from '@shared/api/httpClient.js';
import { createKeyedCachedRequest, stableStringify } from '@shared/lib/cacheUtils.js';

// In-flight deduplication only (cacheMs = 0); React Query owns caching & WebSocket invalidation.
const { requestFn: getPromotionPageDeduped } = createKeyedCachedRequest(
  (requestParams) => api.get('/promotions', { params: requestParams }),
  0,
  stableStringify,
);

export const promotionService = {
  getPage: (params = {}, { force = false } = {}) => {
    const page = Number(params.page || 0);
    const size = Number(params.size || 24);
    return getPromotionPageDeduped({ page, size, sort: 'id,desc', ...params }, { force });
  },
  create: (data) => api.post('/promotions', data),
  update: (id, data) => api.put(`/promotions/${id}`, data),
  delete: (id) => api.delete(`/promotions/${id}`),
};
