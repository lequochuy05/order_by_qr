import api from '@shared/api/httpClient.js';

const VOUCHER_LIST_CACHE_MS = 15_000;

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

const voucherRequests = new Map();
const voucherCache = new Map();

const clearVoucherCache = () => {
  voucherRequests.clear();
  voucherCache.clear();
};

export const voucherService = {
  getPage: async (params = {}, { force = false } = {}) => {
    const page = Number(params.page || 0);
    const size = Number(params.size || 24);
    const requestParams = { page, size, sort: 'id,desc', ...params };
    const key = stableStringify(requestParams);
    const now = Date.now();
    const cached = voucherCache.get(key);
    if (!force && cached && cached.expiresAt > now) return cached.data;
    if (!voucherRequests.has(key)) {
      const request = api
        .get('/vouchers', { params: requestParams })
        .then((res) => {
          voucherCache.set(key, { data: res, expiresAt: Date.now() + VOUCHER_LIST_CACHE_MS });
          return res;
        })
        .finally(() => { voucherRequests.delete(key); });
      voucherRequests.set(key, request);
    }
    return voucherRequests.get(key);
  },
  create: async (data) => { clearVoucherCache(); const res = await api.post('/vouchers', data); clearVoucherCache(); return res; },
  update: async (id, data) => { clearVoucherCache(); const res = await api.put(`/vouchers/${id}`, data); clearVoucherCache(); return res; },
  delete: async (id) => { clearVoucherCache(); await api.delete(`/vouchers/${id}`); clearVoucherCache(); },
};
