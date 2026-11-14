import api from '@shared/api/httpClient.js';
import { createCachedRequest, createKeyedCachedRequest } from '@shared/lib/cacheUtils.js';

const COMBO_LIST_CACHE_MS = 15_000;
const COMBO_DETAIL_CACHE_MS = 10_000;

const stableStringify = (value) => {
  if (!value) return 'all';
  if (Array.isArray(value)) return `[${value.map(stableStringify).join(',')}]`;
  if (typeof value === 'object')
    return `{${Object.keys(value)
      .sort()
      .map((k) => `${k}:${stableStringify(value[k])}`)
      .join(',')}}`;
  return String(value);
};

const normalizePageContent = (data) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  return [];
};

const { requestFn: getComboList, clearCache: clearComboListCache } = createCachedRequest(
  () =>
    api
      .get('/combos', {
        params: { size: 1000, sort: 'displayOrder,asc' },
      })
      .then(normalizePageContent),
  COMBO_LIST_CACHE_MS,
);

const { requestFn: getComboPage, clearCache: clearComboPageCache } = createKeyedCachedRequest(
  (params = {}) => {
    const page = Number(params.page || 0);
    const size = Number(params.size || 24);
    return api.get('/combos', {
      params: { page, size, sort: 'displayOrder,asc', ...params },
    });
  },
  COMBO_LIST_CACHE_MS,
  stableStringify,
);

const { requestFn: getComboDetail, clearCache: clearComboDetailCache } = createKeyedCachedRequest(
  (id) => api.get(`/combos/${id}`),
  COMBO_DETAIL_CACHE_MS,
  (id) => String(id),
);

const clearComboCache = () => {
  clearComboListCache();
  clearComboPageCache();
  clearComboDetailCache();
};

export const comboService = {
  clearCache: clearComboCache,
  getAll: (options) => getComboList(options),
  getPage: (params, options) => getComboPage(params, options),
  getById: (id, options) => getComboDetail(id, options),
  create: async (data) => {
    clearComboCache();
    const res = await api.post('/combos', data);
    clearComboCache();
    return res;
  },
  update: async (id, data) => {
    clearComboCache();
    const res = await api.put(`/combos/${id}`, data);
    clearComboCache();
    return res;
  },
  delete: async (id) => {
    clearComboCache();
    await api.delete(`/combos/${id}`);
    clearComboCache();
  },
  toggleActive: async (id, version) => {
    clearComboCache();
    const res = await api.patch(`/combos/${id}/toggle-active`, null, { params: { version } });
    clearComboCache();
    return res;
  },
};
