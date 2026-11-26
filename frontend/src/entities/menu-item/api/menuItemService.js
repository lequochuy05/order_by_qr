import api from '@shared/api/httpClient.js';
import { createKeyedCachedRequest, stableStringify } from '@shared/lib/cacheUtils.js';
import { normalizePage, normalizePageContent } from '@shared/lib/pagination.js';

const listKey = (categoryId) =>
  categoryId && categoryId !== 'ALL' ? `category:${categoryId}` : 'all';

// In-flight deduplication only (cacheMs = 0); React Query owns caching & WebSocket invalidation.
const { requestFn: getMenuList } = createKeyedCachedRequest(
  (categoryId) =>
    api.get('/menu-items', {
      params: {
        size: 1000,
        sort: 'displayOrder,asc',
        ...(categoryId && categoryId !== 'ALL' ? { categoryId } : {}),
      },
    }),
  0,
  listKey,
);

const { requestFn: getMenuPage } = createKeyedCachedRequest(
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
  0,
  stableStringify,
);

const { requestFn: getMenuDetail } = createKeyedCachedRequest(
  (id) => api.get(`/menu-items/${id}`),
  0,
  (id) => String(id),
);

export const menuItemService = {
  getAll: (categoryId, options) => getMenuList(categoryId, options).then(normalizePageContent),
  getPage: (params, options) => getMenuPage(params, options),
  getById: (id, options) => getMenuDetail(id, options),
  create: (itemData) => api.post('/menu-items', itemData),
  update: (id, itemData) => api.put(`/menu-items/${id}`, itemData),
  toggleAvailable: (id, version) => {
    const config = version != null ? { params: { version } } : undefined;
    return api.patch(`/menu-items/${id}/toggle-available`, null, config);
  },
  uploadImage: (id, version, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`/menu-items/${id}/image`, formData, { params: { version } });
  },
  delete: (id, version) => api.delete(`/menu-items/${id}`, { params: { version } }),
  generateDescription: (itemName, categoryName, price, ingredients) =>
    api.post('/ai/menu-item/description', {
      itemName,
      categoryName,
      price,
      ingredients,
    }),
};
