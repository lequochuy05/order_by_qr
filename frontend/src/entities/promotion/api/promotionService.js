import api from '@shared/api/httpClient.js';

const PROMOTION_LIST_CACHE_MS = 15_000;

const stableStringify = (value) => {
  if (!value) return 'all';
  if (Array.isArray(value)) return `[${value.map(stableStringify).join(',')}]`;
  if (typeof value === 'object') {
    return `{${Object.keys(value)
      .sort()
      .map((k) => `${k}:${stableStringify(value[k])}`)
      .join(',')}}`;
  }
  return String(value);
};

const promotionRequests = new Map();
const promotionCache = new Map();

const clearPromotionCache = () => {
  promotionRequests.clear();
  promotionCache.clear();
};

export const promotionService = {
  clearCache: clearPromotionCache,
  getPage: async (params = {}, { force = false } = {}) => {
    const page = Number(params.page || 0);
    const size = Number(params.size || 24);
    const requestParams = { page, size, sort: 'id,desc', ...params };
    const key = stableStringify(requestParams);
    const now = Date.now();
    const cached = promotionCache.get(key);
    if (!force && cached && cached.expiresAt > now) return cached.data;
    if (!promotionRequests.has(key)) {
      const request = api
        .get('/promotions', { params: requestParams })
        .then((res) => {
          promotionCache.set(key, { data: res, expiresAt: Date.now() + PROMOTION_LIST_CACHE_MS });
          return res;
        })
        .finally(() => {
          promotionRequests.delete(key);
        });
      promotionRequests.set(key, request);
    }
    return promotionRequests.get(key);
  },
  getActive: async () => api.get('/promotions/active'),
  getById: async (id) => api.get(`/promotions/${id}`),
  create: async (data) => {
    clearPromotionCache();
    const res = await api.post('/promotions', data);
    clearPromotionCache();
    return res;
  },
  update: async (id, data) => {
    clearPromotionCache();
    const res = await api.put(`/promotions/${id}`, data);
    clearPromotionCache();
    return res;
  },
  delete: async (id) => {
    clearPromotionCache();
    await api.delete(`/promotions/${id}`);
    clearPromotionCache();
  },
};
