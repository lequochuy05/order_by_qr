import api from '@shared/api/httpClient.js';

const PREVIEW_CACHE_MS = 1_500;
const ORDER_QUERY_CACHE_MS = 5_000;
const MAX_CACHE_SIZE = 50;

let tableBoardRequest = null;
let kitchenOrdersRequest = null;
const orderHistoryRequests = new Map();
const orderHistoryCache = new Map();
const orderAnalyticsRequests = new Map();
const orderAnalyticsCache = new Map();
const previewRequests = new Map();
const previewCache = new Map();

const setCacheWithLimit = (cache, key, value) => {
  if (cache.size >= MAX_CACHE_SIZE) {
    const firstKey = cache.keys().next().value;
    cache.delete(firstKey);
  }
  cache.set(key, value);
};

const stableStringify = (value) => {
  if (Array.isArray(value)) return `[${value.map(stableStringify).join(',')}]`;
  if (value && typeof value === 'object')
    return `{${Object.keys(value).sort().map((k) => `${JSON.stringify(k)}:${stableStringify(value[k])}`).join(',')}}`;
  return JSON.stringify(value);
};

const clearOrderQueryCache = () => {
  orderHistoryRequests.clear();
  orderHistoryCache.clear();
  orderAnalyticsRequests.clear();
  orderAnalyticsCache.clear();
};

export const orderService = {
  getAllOrders: async (params = {}) => { const res = await api.get('/orders', { params }); return res; },

  getOrderHistory: async (params = {}, { force = false } = {}) => {
    const key = stableStringify(params);
    const cached = orderHistoryCache.get(key);
    if (!force && cached && cached.expiresAt > Date.now()) return cached.data;
    if (!orderHistoryRequests.has(key)) {
      orderHistoryRequests.set(
        key,
        api.get('/analytics/orders/history', { params })
          .then((res) => {
            setCacheWithLimit(orderHistoryCache, key, { data: res, expiresAt: Date.now() + ORDER_QUERY_CACHE_MS });
            setTimeout(() => orderHistoryCache.delete(key), ORDER_QUERY_CACHE_MS);
            return res;
          })
          .finally(() => { orderHistoryRequests.delete(key); }),
      );
    }
    return orderHistoryRequests.get(key);
  },

  getOrderAnalytics: async (params = {}, { force = false } = {}) => {
    const key = stableStringify(params);
    const cached = orderAnalyticsCache.get(key);
    if (!force && cached && cached.expiresAt > Date.now()) return cached.data;
    if (!orderAnalyticsRequests.has(key)) {
      orderAnalyticsRequests.set(
        key,
        api.get('/analytics/orders/summary', { params })
          .then((res) => {
            setCacheWithLimit(orderAnalyticsCache, key, { data: res, expiresAt: Date.now() + ORDER_QUERY_CACHE_MS });
            setTimeout(() => orderAnalyticsCache.delete(key), ORDER_QUERY_CACHE_MS);
            return res;
          })
          .finally(() => { orderAnalyticsRequests.delete(key); }),
      );
    }
    return orderAnalyticsRequests.get(key);
  },

  getCurrentOrder: async (tableId) => {
    try { const res = await api.get(`/orders/table/${tableId}/current`); return res; }
    catch { return null; }
  },

  addItemsToOrder: async (data) => { clearOrderQueryCache(); const res = await api.post('/orders', data); clearOrderQueryCache(); return res; },
  updateOrderItem: async (itemId, data) => { clearOrderQueryCache(); const res = await api.patch(`/orders/items/${itemId}`, data); clearOrderQueryCache(); return res; },
  deleteOrderItem: async (itemId) => { clearOrderQueryCache(); await api.delete(`/orders/items/${itemId}`); clearOrderQueryCache(); },
  markItemPrepared: async (itemId, userId = null) => { clearOrderQueryCache(); await api.patch(`/kitchen/items/${itemId}/prepared`, userId ? { userId } : undefined); clearOrderQueryCache(); },

  previewOrder: async (data) => {
    const key = stableStringify({ tableId: data?.tableId, items: data?.items || [], combos: data?.combos || [], voucherCode: data?.voucherCode || null });
    const cachedPreview = previewCache.get(key);
    if (cachedPreview && cachedPreview.expiresAt > Date.now()) return cachedPreview.data;
    if (!previewRequests.has(key)) {
      previewRequests.set(
        key,
        api.post('/orders/preview', data)
          .then((res) => {
            setCacheWithLimit(previewCache, key, { data: res, expiresAt: Date.now() + PREVIEW_CACHE_MS });
            setTimeout(() => previewCache.delete(key), PREVIEW_CACHE_MS);
            return res;
          })
          .finally(() => { previewRequests.delete(key); }),
      );
    }
    return previewRequests.get(key);
  },

  getKitchenOrders: async () => {
    if (!kitchenOrdersRequest) { kitchenOrdersRequest = api.get('/kitchen/orders').finally(() => { kitchenOrdersRequest = null; }); }
    return kitchenOrdersRequest;
  },
  updateItemStatus: async (itemId, status, userId = null) => { clearOrderQueryCache(); const res = await api.patch(`/kitchen/items/${itemId}/status`, userId ? { status, userId } : { status }); clearOrderQueryCache(); return res; },
  getActiveOrders: async () => { const res = await api.get('/orders/active'); return res; },

  getTableBoard: async () => {
    if (!tableBoardRequest) { tableBoardRequest = api.get('/orders/table-board').finally(() => { tableBoardRequest = null; }); }
    return tableBoardRequest;
  },
  reconcileOrder: async (orderId) => { clearOrderQueryCache(); const res = await api.post(`/orders/${orderId}/reconcile`); clearOrderQueryCache(); return res; },
};
