import api from '@shared/api/httpClient.js';
import { sessionHeaderConfig } from '@shared/api/sessionHeaders.js';
import {
  createCachedRequest,
  createKeyedCachedRequest,
  stableStringify,
} from '@shared/lib/cacheUtils.js';

// In-flight deduplication only (cacheMs = 0); React Query owns caching & WebSocket invalidation.
const { requestFn: getOrderHistoryDeduped } = createKeyedCachedRequest(
  (params = {}) => api.get('/analytics/orders/history', { params }),
  0,
  stableStringify,
);

const { requestFn: getOrderAnalyticsDeduped } = createKeyedCachedRequest(
  (params = {}) => api.get('/analytics/orders/summary', { params }),
  0,
  stableStringify,
);

const { requestFn: getCurrentOrderDeduped } = createKeyedCachedRequest(
  (tableId) => api.get(`/orders/table/${tableId}/current`).catch(() => null),
  0,
  (tableId) => String(tableId),
);

const { requestFn: previewOrderDeduped } = createKeyedCachedRequest(
  (data) => api.post('/orders/preview', data),
  0,
  (data) =>
    stableStringify({
      tableId: data?.tableId,
      items: data?.items || [],
      combos: data?.combos || [],
      voucherCode: data?.voucherCode || null,
    }),
);

const { requestFn: getKitchenOrdersDeduped } = createCachedRequest(
  () => api.get('/kitchen/orders'),
  0,
);

export const orderService = {
  getOrderHistory: (params = {}, options) => getOrderHistoryDeduped(params, options),
  getOrderAnalytics: (params = {}, options) => getOrderAnalyticsDeduped(params, options),
  getCurrentOrder: async (tableId, options) => {
    if (!tableId) return null;
    return getCurrentOrderDeduped(tableId, options);
  },
  getOrderById: (id) => api.get(`/orders/${id}`),
  updateOrderStatus: (orderId, status) => api.patch(`/orders/${orderId}/status`, { status }),
  addItemsToOrder: (data) => api.post('/orders', data),
  updateOrderItem: (itemId, data) => api.patch(`/orders/items/${itemId}`, data),
  deleteOrderItem: (itemId, reason = null) =>
    api.delete(`/orders/items/${itemId}`, reason ? { params: { reason } } : undefined),
  cancelOrder: (orderId, reason = null) =>
    api.patch(`/orders/${orderId}/cancel`, reason ? { reason } : undefined),
  markItemPrepared: (itemId, userId = null) =>
    api.patch(`/kitchen/items/${itemId}/prepared`, userId ? { userId } : undefined),
  previewOrder: (data) => previewOrderDeduped(data),
  getKitchenOrders: () => getKitchenOrdersDeduped(),
  updateItemStatus: (itemId, status, userId = null) =>
    api.patch(`/kitchen/items/${itemId}/status`, userId ? { status, userId } : { status }),
  batchUpdateItemStatus: (itemIds, status, userId = null) =>
    api.patch(
      '/kitchen/items/batch-status',
      userId ? { itemIds, status, userId } : { itemIds, status },
    ),
  cancelKitchenItem: (itemId, reason = null) =>
    api.delete(`/kitchen/items/${itemId}`, reason ? { params: { reason } } : undefined),
  createPublicOrder: (orderData) =>
    api.post('/public/orders', orderData, sessionHeaderConfig(orderData?.sessionToken)),
  reconcileOrder: (orderId) => api.post(`/orders/${orderId}/reconcile`),
};
