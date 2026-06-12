import api from '@shared/api/httpClient.js';
import { createKeyedCachedRequest } from '@shared/lib/cacheUtils.js';

const stableStringify = (value) => {
    if (!value) return 'all';
    if (Array.isArray(value)) return `[${value.map(stableStringify).join(',')}]`;
    if (typeof value === 'object') return `{${Object.keys(value).sort().map(k => `${k}:${stableStringify(value[k])}`).join(',')}}`;
    return String(value);
};

const INVENTORY_CACHE_MS = 10_000;

const { requestFn: getItemsCached, clearCache: clearItemsCache } = createKeyedCachedRequest(
  (options = {}) => api.get('/inventory/items', { params: { size: 1000, sort: 'name,asc', ...options } })
    .then(normalizePageContent)
    .then((items) => items.map(normalizeInventoryItem)),
  INVENTORY_CACHE_MS,
  (options) => stableStringify(options)
);

const { requestFn: getMovementsCached, clearCache: clearMovementsCache } = createKeyedCachedRequest(
  (options = {}) => api.get('/inventory/movements', { params: { size: 1000, sort: 'createdAt,desc', ...options } })
    .then(normalizePageContent)
    .then((movements) => movements.map(normalizeStockMovement)),
  INVENTORY_CACHE_MS,
  (options) => stableStringify(options)
);

const { requestFn: getRecipeCached, clearCache: clearRecipeCache } = createKeyedCachedRequest(
  (menuItemId) => api.get(`/inventory/recipes/${menuItemId}`),
  INVENTORY_CACHE_MS,
  (menuItemId) => String(menuItemId)
);

const normalizePageContent = (data) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  return [];
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
    outOfStock: item.outOfStock ?? availableQuantity <= 0
  };
};

const normalizeStockMovement = (movement = {}) => ({
  ...movement,
  movementType: movement.movementType ?? movement.type
});

const inventoryItemPayload = (data = {}) => ({
  name: data.name,
  unit: data.unit,
  lowStockThreshold: Number(data.lowStockThreshold || 0),
  active: data.active !== false
});

const clearAllInventoryCache = () => {
  clearItemsCache();
  clearMovementsCache();
  clearRecipeCache();
};

export const inventoryService = {
  getItems: (options) => getItemsCached(options),
  getMovements: (options) => getMovementsCached(options),
  getRecipe: (menuItemId, options) => getRecipeCached(menuItemId, options),

  createItem: async (data) => {
    clearAllInventoryCache();
    const initialQuantity = Number(data.quantityOnHand || 0);
    const res = await api.post('/inventory/items', inventoryItemPayload(data));
    if (res?.id && initialQuantity > 0) {
      await api.post(`/inventory/items/${res.id}/stock-in`, {
        quantity: initialQuantity,
        note: 'Tồn đầu kỳ'
      });
    }
    clearAllInventoryCache();
    return res;
  },
  
  updateItem: async (id, data) => {
    clearAllInventoryCache();
    const res = await api.put(`/inventory/items/${id}`, inventoryItemPayload(data));
    clearAllInventoryCache();
    return res;
  },
  
  stockIn: async (id, data) => {
    clearAllInventoryCache();
    const res = await api.post(`/inventory/items/${id}/stock-in`, data);
    clearAllInventoryCache();
    return res;
  },
  
  adjust: async (id, data) => {
    clearAllInventoryCache();
    const res = await api.post(`/inventory/items/${id}/adjust`, data);
    clearAllInventoryCache();
    return res;
  },
  
  updateRecipe: async (menuItemId, data) => {
    clearAllInventoryCache();
    const res = await api.put(`/inventory/recipes/${menuItemId}`, data);
    clearAllInventoryCache();
    return res;
  }
};
