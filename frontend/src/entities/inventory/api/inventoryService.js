import api from '@shared/api/httpClient.js';
import {
  createCachedRequest,
  createKeyedCachedRequest,
  stableStringify,
} from '@shared/lib/cacheUtils.js';
import { normalizePage, normalizePageContent } from '@shared/lib/pagination.js';

const normalizeInventoryItem = (item = {}) => {
  const quantityOnHand = Number(item.quantityOnHand ?? 0);
  const availableQuantity = Number(item.availableQuantity ?? quantityOnHand);
  const lowStockThreshold = Number(item.lowStockThreshold ?? 0);
  const unitCost = Number(item.unitCost ?? 0);
  return {
    ...item,
    quantityOnHand,
    availableQuantity,
    lowStockThreshold,
    unitCost,
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
  unitCost: Number(data.unitCost || 0),
  active: data.active !== false,
  version: data.version ?? null,
});

// In-flight deduplication only (cacheMs = 0); React Query owns caching & WebSocket invalidation.
const { requestFn: getActiveItemsDeduped } = createCachedRequest(
  () =>
    api
      .get('/inventory/items/active')
      .then((items) => (Array.isArray(items) ? items.map(normalizeInventoryItem) : [])),
  0,
);

const { requestFn: getItemsDeduped } = createKeyedCachedRequest(
  (options = {}) =>
    api
      .get('/inventory/items', { params: { size: 1000, sort: 'name,asc', ...options } })
      .then(normalizePageContent)
      .then((items) => items.map(normalizeInventoryItem)),
  0,
  stableStringify,
);

const { requestFn: getItemPageDeduped } = createKeyedCachedRequest(
  (options = {}) => {
    const page = Number(options.page || 0);
    const size = Number(options.size || 24);
    return api
      .get('/inventory/items', { params: { page, size, sort: 'name,asc', ...options } })
      .then((data) => normalizePage(data, page, size, normalizeInventoryItem));
  },
  0,
  stableStringify,
);

const { requestFn: getSummaryDeduped } = createCachedRequest(
  () => api.get('/inventory/items/summary'),
  0,
);

const { requestFn: getMovementsDeduped } = createKeyedCachedRequest(
  (options = {}) =>
    api
      .get('/inventory/movements', { params: { size: 100, sort: 'createdAt,desc', ...options } })
      .then(normalizePageContent)
      .then((movements) => movements.map(normalizeStockMovement)),
  0,
  stableStringify,
);

const { requestFn: getRecipeDeduped } = createKeyedCachedRequest(
  (menuItemId) => api.get(`/inventory/recipes/${menuItemId}`),
  0,
  (menuItemId) => String(menuItemId),
);

export const inventoryService = {
  getActiveItems: ({ force = false } = {}) => getActiveItemsDeduped({ force }),
  getItems: ({ force = false, ...params } = {}) => getItemsDeduped(params, { force }),
  getItemPage: ({ force = false, ...params } = {}) => getItemPageDeduped(params, { force }),
  getSummary: ({ force = false } = {}) => getSummaryDeduped({ force }),
  getMovements: ({ force = false, ...params } = {}) => getMovementsDeduped(params, { force }),
  getRecipe: (menuItemId, options) => getRecipeDeduped(menuItemId, options),

  createItem: async (data) => {
    const initialQuantity = Number(data.quantityOnHand || 0);
    const res = await api.post('/inventory/items', inventoryItemPayload(data));
    if (res?.id && initialQuantity > 0) {
      await api.post(`/inventory/items/${res.id}/stock-in`, {
        quantity: initialQuantity,
        note: 'Tồn đầu kỳ',
      });
    }
    return res;
  },

  updateItem: (id, data) => api.put(`/inventory/items/${id}`, inventoryItemPayload(data)),
  stockIn: (id, data) => api.post(`/inventory/items/${id}/stock-in`, data),
  batchStockIn: (data) => api.post('/inventory/items/batch-stock-in', data),
  adjust: (id, data) => api.post(`/inventory/items/${id}/adjust`, data),
  updateRecipe: (menuItemId, data) => api.put(`/inventory/recipes/${menuItemId}`, data),
};
