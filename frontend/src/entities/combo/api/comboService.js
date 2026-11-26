import api from '@shared/api/httpClient.js';
import {
  createCachedRequest,
  createKeyedCachedRequest,
  stableStringify,
} from '@shared/lib/cacheUtils.js';
import { normalizePageContent } from '@shared/lib/pagination.js';

// In-flight deduplication only (cacheMs = 0); React Query owns caching & WebSocket invalidation.
const { requestFn: getComboList } = createCachedRequest(
  () =>
    api
      .get('/combos', {
        params: { size: 1000, sort: 'displayOrder,asc' },
      })
      .then(normalizePageContent),
  0,
);

const { requestFn: getComboPage } = createKeyedCachedRequest(
  (params = {}) => {
    const page = Number(params.page || 0);
    const size = Number(params.size || 24);
    return api.get('/combos', {
      params: { page, size, sort: 'displayOrder,asc', ...params },
    });
  },
  0,
  stableStringify,
);

const { requestFn: getComboDetail } = createKeyedCachedRequest(
  (id) => api.get(`/combos/${id}`),
  0,
  (id) => String(id),
);

export const comboService = {
  getAll: (options) => getComboList(options),
  getPage: (params, options) => getComboPage(params, options),
  getById: (id, options) => getComboDetail(id, options),
  create: (data) => api.post('/combos', data),
  update: (id, data) => api.put(`/combos/${id}`, data),
  delete: (id, version) => api.delete(`/combos/${id}`, { params: { version } }),
  toggleActive: (id, version) => api.patch(`/combos/${id}/toggle-active`, null, { params: { version } }),
  toggleAvailable: (id, version) => {
    const config = version != null ? { params: { version } } : undefined;
    return api.patch(`/combos/${id}/toggle-available`, null, config);
  },
};
