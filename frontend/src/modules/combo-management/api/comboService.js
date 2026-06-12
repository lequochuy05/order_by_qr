import api from '@shared/api/httpClient.js';

const COMBO_LIST_CACHE_MS = 15_000;
const COMBO_DETAIL_CACHE_MS = 10_000;

let comboListRequest = null;
let comboListCache = {
  data: null,
  expiresAt: 0
};
const comboDetailRequests = new Map();
const comboDetailCache = new Map();

const clearComboCache = () => {
  comboListRequest = null;
  comboListCache = {
    data: null,
    expiresAt: 0
  };
  comboDetailRequests.clear();
  comboDetailCache.clear();
};

export const comboService = {
  // Lấy toàn bộ combo
  getAll: async ({ force = false } = {}) => {
    const now = Date.now();
    if (!force && comboListCache.data && comboListCache.expiresAt > now) {
      return comboListCache.data;
    }

    if (!comboListRequest) {
      comboListRequest = api.get('/combos')
        .then((res) => {
          comboListCache = {
            data: res,
            expiresAt: Date.now() + COMBO_LIST_CACHE_MS
          };
          return res;
        })
        .finally(() => {
          comboListRequest = null;
        });
    }

    return comboListRequest;
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
      const request = api.get(`/combos/${id}`)
        .then((res) => {
          comboDetailCache.set(key, {
            data: res,
            expiresAt: Date.now() + COMBO_DETAIL_CACHE_MS
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
  }
};
