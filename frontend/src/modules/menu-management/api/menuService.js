import api from '@shared/api/httpClient.js';
import { createKeyedCachedRequest } from '@shared/lib/cacheUtils.js';

const MENU_LIST_CACHE_MS = 15_000;
const MENU_DETAIL_CACHE_MS = 10_000;

const listKey = (categoryId) => (categoryId && categoryId !== 'ALL' ? `category:${categoryId}` : 'all');

const { requestFn: getMenuList, clearCache: clearMenuListCache } = createKeyedCachedRequest(
  (categoryId) => api.get('/menu-items', {
    params: categoryId && categoryId !== 'ALL' ? { categoryId } : undefined
  }),
  MENU_LIST_CACHE_MS,
  listKey
);

const { requestFn: getMenuDetail, clearCache: clearMenuDetailCache } = createKeyedCachedRequest(
  (id) => api.get(`/menu-items/${id}`),
  MENU_DETAIL_CACHE_MS,
  (id) => String(id)
);

const clearMenuCache = () => {
  clearMenuListCache();
  clearMenuDetailCache();
};

export const menuItemService = {
  // Lấy tất cả hoặc theo danh mục
  getAll: (categoryId, options) => getMenuList(categoryId, options),

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
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    clearMenuCache();
    return res;
  },

  // Xóa món ăn
  delete: async (id) => {
    clearMenuCache();
    await api.delete(`/menu-items/${id}`);
    clearMenuCache();
  }
};
