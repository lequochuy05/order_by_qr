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
    api.get('/menu-items/management-summary', {
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
      .get('/menu-items/management-summary', {
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
  // Lấy tất cả hoặc theo danh mục
  getAll: (categoryId, options) => getMenuList(categoryId, options).then(normalizePageContent),

  getPage: (params, options) => getMenuPage(params, options),

  getById: (id, options) => getMenuDetail(id, options),

  // Tạo mới món ăn
  create: async (itemData) => {
    clearMenuCache();
    const res = await api.post('/menu-items', itemData);
    clearMenuCache();
    return res;
  },

  // Cập nhật món ăn
  update: async (id, itemData) => {
    clearMenuCache();
    const res = await api.put(`/menu-items/${id}`, itemData);
    clearMenuCache();
    return res;
  },

  // Upload ảnh món ăn
  uploadImage: async (id, file) => {
    clearMenuCache();
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post(`/menu-items/${id}/image`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    clearMenuCache();
    return res;
  },

  // Xóa món ăn
  delete: async (id) => {
    clearMenuCache();
    await api.delete(`/menu-items/${id}`);
    clearMenuCache();
  },

  // AI Generate description
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
