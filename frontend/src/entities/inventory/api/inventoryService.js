import api from '@shared/api/httpClient.js';
import { createCachedRequest, createKeyedCachedRequest } from '@shared/lib/cacheUtils.js';

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

const INVENTORY_CACHE_MS = 10_000;

const normalizePageContent = (data) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  return [];
};

const emptyPage = (page = 0, size = 24) => ({
  content: [], number: page, size, totalElements: 0, totalPages: 0,
});

const normalizePage = (data, page = 0, size = 24, itemMapper = (item) => item) => {
  if (Array.isArray(data)) {
    return { ...emptyPage(page, size), content: data.map(itemMapper), totalElements: data.length, totalPages: data.length > 0 ? 1 : 0 };
  }
  return { ...emptyPage(page, size), ...data, content: normalizePageContent(data).map(itemMapper) };
};

const normalizeInventoryItem = (item = {}) => {
  const quantityOnHand = Number(item.quantityOnHand ?? 0);
  const availableQuantity = Number(item.availableQuantity ?? quantityOnHand);
  const lowStockThreshold = Number(item.lowStockThreshold ?? 0);
  return {
    ...item,
    quantityOnHand,
    availableQuantity,
    lowStockThreshold,
    lowStock: item.lowStock ?? (lowStockThreshold > 0 && availableQuantity <= lowStockThreshold),
    outOfStock: item.outOfStock ?? availableQuantity <= 0,
  };
};

const normalizeStockMovement = (movement = {}) => ({
  ...movement,
  movementType: movement.movementType ?? movement.type,
});

const inventoryItemPayload = (data = {}) => ({
  name: data.name,
  unit: data.unit,
  lowStockThreshold: Number(data.lowStockThreshold || 0),
  active: data.active !== false,
});

const { requestFn: getItemsCached, clearCache: clearItemsCache } = createKeyedCachedRequest(
  (options = {}) =>
    api.get('/inventory/items', { params: { size: 1000, sort: 'name,asc', ...options } })
      .then(normalizePageContent)
      .then((items) => items.map(normalizeInventoryItem)),
  INVENTORY_CACHE_MS,
  (options) => stableStringify(options),
);

const { requestFn: getItemPageCached, clearCache: clearItemPageCache } = createKeyedCachedRequest(
  (options = {}) => {
    const page = Number(options.page || 0);
    const size = Number(options.size || 24);
    return api.get('/inventory/items', { params: { page, size, sort: 'name,asc', ...options } })
      .then((data) => normalizePage(data, page, size, normalizeInventoryItem));
  },
  INVENTORY_CACHE_MS,
  (options) => stableStringify(options),
);

const { requestFn: getSummaryCached, clearCache: clearSummaryCache } = createCachedRequest(
  () => api.get('/inventory/items/summary'),
  INVENTORY_CACHE_MS,
);

const { requestFn: getMovementsCached, clearCache: clearMovementsCache } = createKeyedCachedRequest(
  (options = {}) =>
    api.get('/inventory/movements', { params: { size: 100, sort: 'createdAt,desc', ...options } })
      .then(normalizePageContent)
      .then((movements) => movements.map(normalizeStockMovement)),
  INVENTORY_CACHE_MS,
  (options) => stableStringify(options),
);

const { requestFn: getRecipeCached, clearCache: clearRecipeCache } = createKeyedCachedRequest(
  (menuItemId) => api.get(`/inventory/recipes/${menuItemId}`),
  INVENTORY_CACHE_MS,
  (menuItemId) => String(menuItemId),
);

const clearAllInventoryCache = () => {
  clearItemsCache();
  clearItemPageCache();
  clearMovementsCache();
  clearRecipeCache();
  clearSummaryCache();
};

export const inventoryService = {
  getItems: ({ force = false, ...params } = {}) => getItemsCached(params, { force }),
  getItemPage: ({ force = false, ...params } = {}) => getItemPageCached(params, { force }),
  getSummary: ({ force = false } = {}) => getSummaryCached({ force }),
  getMovements: ({ force = false, ...params } = {}) => getMovementsCached(params, { force }),
  getRecipe: (menuItemId, options) => getRecipeCached(menuItemId, options),

  createItem: async (data) => {
    clearAllInventoryCache();
    const initialQuantity = Number(data.quantityOnHand || 0);
    const res = await api.post('/inventory/items', inventoryItemPayload(data));
    if (res?.id && initialQuantity > 0) {
      await api.post(`/inventory/items/${res.id}/stock-in`, { quantity: initialQuantity, note: 'Tồn đầu kỳ' });
    }
    clearAllInventoryCache();
    return res;
  },

  updateItem: async (id, data) => { clearAllInventoryCache(); const res = await api.put(`/inventory/items/${id}`, inventoryItemPayload(data)); clearAllInventoryCache(); return res; },
  stockIn: async (id, data) => { clearAllInventoryCache(); const res = await api.post(`/inventory/items/${id}/stock-in`, data); clearAllInventoryCache(); return res; },
  adjust: async (id, data) => { clearAllInventoryCache(); const res = await api.post(`/inventory/items/${id}/adjust`, data); clearAllInventoryCache(); return res; },
  updateRecipe: async (menuItemId, data) => { clearAllInventoryCache(); const res = await api.put(`/inventory/recipes/${menuItemId}`, data); clearAllInventoryCache(); return res; },
};
