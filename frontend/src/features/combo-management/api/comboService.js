import api from '@shared/api/httpClient.js';

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

let comboListRequest = null;
let comboListCache = {
  data: null,
  expiresAt: 0,
};
const comboDetailRequests = new Map();
const comboDetailCache = new Map();
const comboPageRequests = new Map();
const comboPageCache = new Map();

const clearComboCache = () => {
  comboListRequest = null;
  comboListCache = {
    data: null,
    expiresAt: 0,
  };
  comboDetailRequests.clear();
  comboDetailCache.clear();
  comboPageRequests.clear();
  comboPageCache.clear();
};

export const comboService = {
  // Lấy toàn bộ combo
  getAll: async ({ force = false } = {}) => {
    const now = Date.now();
    if (!force && comboListCache.data && comboListCache.expiresAt > now) {
      return comboListCache.data;
    }

    if (!comboListRequest) {
      comboListRequest = api
        .get('/combos/management-summary', {
          params: { size: 1000, sort: 'displayOrder,asc' },
        })
        .then((res) => {
          const data = normalizePageContent(res);
          comboListCache = {
            data,
            expiresAt: Date.now() + COMBO_LIST_CACHE_MS,
          };
          return data;
        })
        .finally(() => {
          comboListRequest = null;
        });
    }

    return comboListRequest;
  },

  getPage: async (params = {}, { force = false } = {}) => {
    const page = Number(params.page || 0);
    const size = Number(params.size || 24);
    const requestParams = {
      page,
      size,
      sort: 'displayOrder,asc',
      ...params,
    };
    const key = stableStringify(requestParams);
    const now = Date.now();
    const cached = comboPageCache.get(key);
    if (!force && cached && cached.expiresAt > now) {
      return cached.data;
    }

    if (!comboPageRequests.has(key)) {
      const request = api
        .get('/combos/management-summary', { params: requestParams })
        .then((res) => {
          comboPageCache.set(key, {
            data: res,
            expiresAt: Date.now() + COMBO_LIST_CACHE_MS,
          });
          return res;
        })
        .finally(() => {
          comboPageRequests.delete(key);
        });
      comboPageRequests.set(key, request);
    }

    return comboPageRequests.get(key);
  },

  // Lấy chi tiết combo theo ID
  getById: async (id, { force = false } = {}) => {
    const key = String(id);
    const now = Date.now();
    const cached = comboDetailCache.get(key);
    if (!force && cached && cached.expiresAt > now) {
      return cached.data;
    }

    if (!comboDetailRequests.has(key)) {
      const request = api
        .get(`/combos/${id}`)
        .then((res) => {
          comboDetailCache.set(key, {
            data: res,
            expiresAt: Date.now() + COMBO_DETAIL_CACHE_MS,
          });
          return res;
        })
        .finally(() => {
          comboDetailRequests.delete(key);
        });
      comboDetailRequests.set(key, request);
    }

    return comboDetailRequests.get(key);
  },

  // Tạo mới combo
  create: async (data) => {
    clearComboCache();
    const res = await api.post('/combos', data);
    clearComboCache();
    return res;
  },

  // Cập nhật combo
  update: async (id, data) => {
    clearComboCache();
    const res = await api.put(`/combos/${id}`, data);
    clearComboCache();
    return res;
  },

  // Xóa combo
  delete: async (id) => {
    clearComboCache();
    await api.delete(`/combos/${id}`);
    clearComboCache();
  },

  // Bật/tắt trạng thái kinh doanh
  toggleActive: async (id) => {
    clearComboCache();
    const res = await api.patch(`/combos/${id}/toggle-active`);
    clearComboCache();
    return res;
  },
};
