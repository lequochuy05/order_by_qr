import api from '@shared/api/httpClient.js';
import { createKeyedCachedRequest, stableStringify } from '@shared/lib/cacheUtils.js';

// In-flight deduplication only (cacheMs = 0); React Query owns caching & WebSocket invalidation.
const { requestFn: getVoucherPageDeduped } = createKeyedCachedRequest(
  (requestParams) => api.get('/vouchers', { params: requestParams }),
  0,
  stableStringify,
);

export const voucherService = {
  getPage: (params = {}, { force = false } = {}) => {
    const page = Number(params.page || 0);
    const size = Number(params.size || 24);
    return getVoucherPageDeduped({ page, size, sort: 'id,desc', ...params }, { force });
  },
  create: (data) => api.post('/vouchers', data),
  update: (id, data) => api.put(`/vouchers/${id}`, data),
  delete: (id) => api.delete(`/vouchers/${id}`),
};
