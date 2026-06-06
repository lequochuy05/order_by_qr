import api from '@shared/api/httpClient.js';

export const orderService = {
    getAllOrders: async (params = {}) => {
        const res = await api.get('/orders', { params });
        return res;
    },

    // Paginated history with server-side filtering
    getOrderHistory: async (params = {}) => {
        const res = await api.get('/orders/history', { params });
        return res;
    },

    // Aggregate stats for filtered period
    getOrderStats: async (params = {}) => {
        const res = await api.get('/orders/stats', { params });
        return res;
    },

    getCurrentOrder: async(tableId) => {
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
        const res = await api.post('/orders', data);
        return res;
    },

    // Cập nhật số lượng/ghi chú món
    updateOrderItem: async (itemId, data) => {
        // data: { quantity, notes }
        const res = await api.patch(`/orders/items/${itemId}`, data);
        return res;
    },

    // Hủy món
    deleteOrderItem: async (itemId) => {
        await api.delete(`/orders/items/${itemId}`);
    },

    // Đánh dấu đã phục vụ (bếp xong)
    markItemPrepared: async (itemId) => {
        await api.patch(`/kitchen/items/${itemId}/prepared`);
    },

    // Xem trước hóa đơn (để tính Voucher)
    previewOrder: async (data) => {
        // data: { tableId, items, combos, voucherCode }
        const res = await api.post('/orders/preview', data);
        return res;
    },

    // Thanh toán
    payOrder: async (orderId, userId, voucherCode = null) => {
        const payload = { userId };
        if (voucherCode) {
            payload.voucherCode = voucherCode;
        }
        const res = await api.post(`/orders/${orderId}/pay`, payload);
        return res;
    },

    // Lấy danh sách đơn cho nhà bếp
    getKitchenOrders: async () => {
        const res = await api.get('/kitchen/orders');
        return res;
    },

    // Cập nhật trạng thái món (PENDING, COOKING, FINISHED)
    updateItemStatus: async (itemId, status) => {
        const res = await api.patch(`/kitchen/items/${itemId}/status`, { status });
        return res;
    },

    getActiveOrders: async () => {
        const res = await api.get('/orders/active');
        return res;
    },

    reconcileOrder: async (orderId) => {
        const res = await api.post(`/orders/${orderId}/reconcile`);
        return res;
    }
};
