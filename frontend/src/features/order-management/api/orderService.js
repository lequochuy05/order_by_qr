import api from '@shared/api/httpClient.js';

let tableBoardRequest = null;
let kitchenOrdersRequest = null;
const orderHistoryRequests = new Map();
const orderHistoryCache = new Map();
const orderAnalyticsRequests = new Map();
const orderAnalyticsCache = new Map();
const previewRequests = new Map();
const previewCache = new Map();
const ORDER_QUERY_CACHE_MS = 5_000;
const PREVIEW_CACHE_MS = 1_500;
const MAX_CACHE_SIZE = 50;

const setCacheWithLimit = (cache, key, value) => {
  if (cache.size >= MAX_CACHE_SIZE) {
    const firstKey = cache.keys().next().value;
    cache.delete(firstKey);
  }
  cache.set(key, value);
};

const stableStringify = (value) => {
  if (Array.isArray(value)) {
    return `[${value.map(stableStringify).join(',')}]`;
  }
  if (value && typeof value === 'object') {
    return `{${Object.keys(value)
      .sort()
      .map((key) => `${JSON.stringify(key)}:${stableStringify(value[key])}`)
      .join(',')}}`;
  }
  return JSON.stringify(value);
};

const clearOrderQueryCache = () => {
  orderHistoryRequests.clear();
  orderHistoryCache.clear();
  orderAnalyticsRequests.clear();
  orderAnalyticsCache.clear();
};

export const orderService = {
  getAllOrders: async (params = {}) => {
    const res = await api.get('/orders', { params });
    return res;
  },

  // Paginated history with server-side filtering
  getOrderHistory: async (params = {}, { force = false } = {}) => {
    const key = stableStringify(params);
    const cached = orderHistoryCache.get(key);
    if (!force && cached && cached.expiresAt > Date.now()) {
      return cached.data;
    }

    if (!orderHistoryRequests.has(key)) {
      orderHistoryRequests.set(
        key,
        api
          .get('/orders/history', { params })
          .then((res) => {
            setCacheWithLimit(orderHistoryCache, key, {
              data: res,
              expiresAt: Date.now() + ORDER_QUERY_CACHE_MS,
            });
            setTimeout(() => orderHistoryCache.delete(key), ORDER_QUERY_CACHE_MS);
            return res;
          })
          .finally(() => {
            orderHistoryRequests.delete(key);
          }),
      );
    }

    return orderHistoryRequests.get(key);
  },

  // Aggregate analytics for filtered period
  getOrderAnalytics: async (params = {}, { force = false } = {}) => {
    const key = stableStringify(params);
    const cached = orderAnalyticsCache.get(key);
    if (!force && cached && cached.expiresAt > Date.now()) {
      return cached.data;
    }

    if (!orderAnalyticsRequests.has(key)) {
      orderAnalyticsRequests.set(
        key,
        api
          .get('/orders/analytics', { params })
          .then((res) => {
            setCacheWithLimit(orderAnalyticsCache, key, {
              data: res,
              expiresAt: Date.now() + ORDER_QUERY_CACHE_MS,
            });
            setTimeout(() => orderAnalyticsCache.delete(key), ORDER_QUERY_CACHE_MS);
            return res;
          })
          .finally(() => {
            orderAnalyticsRequests.delete(key);
          }),
      );
    }

    return orderAnalyticsRequests.get(key);
  },

  getCurrentOrder: async (tableId) => {
    try {
      const res = await api.get(`/orders/table/${tableId}/current`);
      return res;
    } catch {
      return null;
    }
  },

  // Thêm món vào bàn (Tạo đơn mới hoặc update đơn cũ)
  addItemsToOrder: async (data) => {
    // data: { tableId, items: [...], combos: [...] }
    clearOrderQueryCache();
    const res = await api.post('/orders', data);
    clearOrderQueryCache();
    return res;
  },

  // Cập nhật số lượng/ghi chú món
  updateOrderItem: async (itemId, data) => {
    // data: { quantity, notes }
    clearOrderQueryCache();
    const res = await api.patch(`/orders/items/${itemId}`, data);
    clearOrderQueryCache();
    return res;
  },

  // Hủy món
  deleteOrderItem: async (itemId) => {
    clearOrderQueryCache();
    await api.delete(`/orders/items/${itemId}`);
    clearOrderQueryCache();
  },

  // Đánh dấu đã phục vụ (bếp xong)
  markItemPrepared: async (itemId, userId = null) => {
    const payload = userId ? { userId } : undefined;
    clearOrderQueryCache();
    await api.patch(`/kitchen/items/${itemId}/prepared`, payload);
    clearOrderQueryCache();
  },

  // Xem trước hóa đơn (để tính Voucher)
  previewOrder: async (data) => {
    // data: { tableId, items, combos, voucherCode }
    const key = stableStringify({
      tableId: data?.tableId,
      items: data?.items || [],
      combos: data?.combos || [],
      voucherCode: data?.voucherCode || null,
    });
    const cachedPreview = previewCache.get(key);
    if (cachedPreview && cachedPreview.expiresAt > Date.now()) {
      return cachedPreview.data;
    }

    if (!previewRequests.has(key)) {
      previewRequests.set(
        key,
        api
          .post('/orders/preview', data)
          .then((res) => {
            setCacheWithLimit(previewCache, key, {
              data: res,
              expiresAt: Date.now() + PREVIEW_CACHE_MS,
            });
            // Automatically clear expired entries
            setTimeout(() => previewCache.delete(key), PREVIEW_CACHE_MS);
            return res;
          })
          .finally(() => {
            previewRequests.delete(key);
          }),
      );
    }

    return previewRequests.get(key);
  },

  // Thanh toán
  payOrder: async (orderId, voucherCode = null) => {
    const payload = {};
    if (voucherCode) {
      payload.voucherCode = voucherCode;
    }
    clearOrderQueryCache();
    const res = await api.post(`/orders/${orderId}/pay`, payload);
    clearOrderQueryCache();
    return res;
  },

  // Lấy danh sách đơn cho nhà bếp
  getKitchenOrders: async () => {
    if (!kitchenOrdersRequest) {
      kitchenOrdersRequest = api.get('/kitchen/orders').finally(() => {
        kitchenOrdersRequest = null;
      });
    }
    return kitchenOrdersRequest;
  },

  // Cập nhật trạng thái món (PENDING, COOKING, FINISHED)
  updateItemStatus: async (itemId, status, userId = null) => {
    const payload = { status };
    if (userId) payload.userId = userId;
    clearOrderQueryCache();
    const res = await api.patch(`/kitchen/items/${itemId}/status`, payload);
    clearOrderQueryCache();
    return res;
  },

  getActiveOrders: async () => {
    const res = await api.get('/orders/active');
    return res;
  },

  getTableBoard: async () => {
    if (!tableBoardRequest) {
      tableBoardRequest = api.get('/orders/table-board').finally(() => {
        tableBoardRequest = null;
      });
    }
    return tableBoardRequest;
  },

  reconcileOrder: async (orderId) => {
    clearOrderQueryCache();
    const res = await api.post(`/orders/${orderId}/reconcile`);
    clearOrderQueryCache();
    return res;
  },
};
