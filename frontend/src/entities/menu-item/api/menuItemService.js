import api from '@shared/api/httpClient.js';
import { createKeyedCachedRequest } from '@shared/lib/cacheUtils.js';

const MENU_LIST_CACHE_MS = 15_000;
const MENU_DETAIL_CACHE_MS = 10_000;

const listKey = (categoryId) =>
  categoryId && categoryId !== 'ALL' ? `category:${categoryId}` : 'all';

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

const emptyPage = (page = 0, size = 24) => ({
  content: [],
  number: page,
  size,
  totalElements: 0,
  totalPages: 0,
});

const normalizePage = (data, page = 0, size = 24) => {
  if (Array.isArray(data)) {
    return {
      ...emptyPage(page, size),
      content: data,
      totalElements: data.length,
      totalPages: data.length > 0 ? 1 : 0,
    };
  }
  return {
    ...emptyPage(page, size),
    ...data,
    content: normalizePageContent(data),
  };
};

const { requestFn: getMenuList, clearCache: clearMenuListCache } = createKeyedCachedRequest(
  (categoryId) =>
    api.get('/menu-items', {
      params: {
        size: 1000,
        sort: 'displayOrder,asc',
        ...(categoryId && categoryId !== 'ALL' ? { categoryId } : {}),
      },
    }),
  MENU_LIST_CACHE_MS,
  listKey,
);

const { requestFn: getMenuPage, clearCache: clearMenuPageCache } = createKeyedCachedRequest(
  (params = {}) => {
    const page = Number(params.page || 0);
    const size = Number(params.size || 24);
    return api
      .get('/menu-items', {
        params: {
          page,
          size,
          sort: 'displayOrder,asc',
          ...params,
        },
      })
      .then((data) => normalizePage(data, page, size));
  },
  MENU_LIST_CACHE_MS,
  stableStringify,
);

const { requestFn: getMenuDetail, clearCache: clearMenuDetailCache } = createKeyedCachedRequest(
  (id) => api.get(`/menu-items/${id}`),
  MENU_DETAIL_CACHE_MS,
  (id) => String(id),
);

const clearMenuCache = () => {
  clearMenuListCache();
  clearMenuPageCache();
  clearMenuDetailCache();
};

export const menuItemService = {
  // Cache invalidation fix: clearCache was defined but NOT exported, causing
  // WebSocketInvalidator's menuItemService.clearCache?.() to silently do nothing.
  clearCache: clearMenuCache,
  getAll: (categoryId, options) => getMenuList(categoryId, options).then(normalizePageContent),
  getPage: (params, options) => getMenuPage(params, options),
  getById: (id, options) => getMenuDetail(id, options),
  create: async (itemData) => {
    clearMenuCache();
    const res = await api.post('/menu-items', itemData);
    clearMenuCache();
    return res;
  },
  update: async (id, itemData) => {
    clearMenuCache();
    const res = await api.put(`/menu-items/${id}`, itemData);
    clearMenuCache();
    return res;
  },
  toggleAvailable: async (id, version) => {
    clearMenuCache();
    const config = version != null ? { params: { version } } : undefined;
    const res = await api.patch(`/menu-items/${id}/toggle-available`, null, config);
    clearMenuCache();
    return res;
  },
  uploadImage: async (id, version, file) => {
    clearMenuCache();
    const formData = new FormData();
    formData.append('file', file);
    // M-fix: version required by backend to guard against concurrent image overwrites.
    const res = await api.post(`/menu-items/${id}/image`, formData, { params: { version } });
    clearMenuCache();
    return res;
  },
  delete: async (id, version) => {
    clearMenuCache();
    // L-fix: version required by backend DELETE endpoint for optimistic locking.
    await api.delete(`/menu-items/${id}`, { params: { version } });
    clearMenuCache();
  },
  generateDescription: async (itemName, categoryName, price, ingredients) => {
    const res = await api.post('/ai/menu-item/description', {
      itemName,
      categoryName,
      price,
      ingredients,
    });
    return res;
  },
};
