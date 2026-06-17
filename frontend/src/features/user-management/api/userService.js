import api from '@shared/api/httpClient.js';

const STAFF_LIST_CACHE_MS = 15_000;

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

const userRequests = new Map();
const userCache = new Map();

const clearUserCache = () => {
  userRequests.clear();
  userCache.clear();
};

export const userService = {
  getPage: async (params = {}, { force = false } = {}) => {
    const page = Number(params.page || 0);
    const size = Number(params.size || 24);
    const requestParams = {
      page,
      size,
      sort: 'fullName,asc',
      ...params,
    };
    const key = stableStringify(requestParams);
    const now = Date.now();
    const cached = userCache.get(key);
    if (!force && cached && cached.expiresAt > now) {
      return cached.data;
    }

    if (!userRequests.has(key)) {
      const request = api
        .get('/users', { params: requestParams })
        .then((res) => {
          userCache.set(key, {
            data: res,
            expiresAt: Date.now() + STAFF_LIST_CACHE_MS,
          });
          return res;
        })
        .finally(() => {
          userRequests.delete(key);
        });
      userRequests.set(key, request);
    }

    return userRequests.get(key);
  },

  // Tạo mới (trả về UserDto có id)
  create: async (data) => {
    clearUserCache();
    const res = await api.post('/users', data);
    clearUserCache();
    return res;
  },

  // Cập nhật thông tin
  update: async (id, data) => {
    clearUserCache();
    const res = await api.put(`/users/${id}`, data);
    clearUserCache();
    return res;
  },

  // Upload Avatar (API riêng biệt)
  uploadAvatar: async (id, file) => {
    clearUserCache();
    const formData = new FormData();
    formData.append('file', file);

    const res = await api.post(`/users/${id}/avatar`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    clearUserCache();
    return res;
  },

  // Xóa
  delete: async (id) => {
    clearUserCache();
    await api.delete(`/users/${id}`);
    clearUserCache();
  },
};
