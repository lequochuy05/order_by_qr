import api from '../api';

export const orderService = {
    getAllOrders: async (params = {}) => {
        const res = await api.get('/orders', { params });
        return res.data;
    },

    // Paginated history with server-side filtering
    getOrderHistory: async (params = {}) => {
        const res = await api.get('/orders/history', { params });
        return res.data;
    },

    // Aggregate stats for filtered period
    getOrderStats: async (params = {}) => {
        const res = await api.get('/orders/stats', { params });
        return res.data;
    },

    getCurrentOrder: async(tableId) => {
        try {
            const res = await api.get(`/orders/table/${tableId}/current`);
            return res.status === 200 ? res.data : null;
        } catch {
            return null;
        }
    },

    // Thêm món vào bàn (Tạo đơn mới hoặc update đơn cũ)
    addItemsToOrder: async (data) => {
        // data: { tableId, items: [...], combos: [...] }
        const res = await api.post('/orders', data);
        return res.data;
    },

    // Cập nhật số lượng/ghi chú món
    updateOrderItem: async (itemId, data) => {
        // data: { quantity, notes }
        const res = await api.patch(`/orders/items/${itemId}`, data);
        return res.data;
    },

    // Hủy món
    deleteOrderItem: async (itemId) => {
        await api.delete(`/orders/items/${itemId}`);
    },

    // Đánh dấu đã phục vụ (bếp xong)
    markItemPrepared: async (itemId) => {
        await api.patch(`/orders/items/${itemId}/prepared`);
    },

    // Xem trước hóa đơn (để tính Voucher)
    previewOrder: async (data) => {
        // data: { tableId, items, combos, voucherCode }
        const res = await api.post('/orders/preview', data);
        return res.data;
    },

    // Thanh toán
    payOrder: async (orderId, userId, voucherCode = null) => {
        let url = `/orders/${orderId}/pay?userId=${userId}`;
        if (voucherCode) url += `&voucherCode=${encodeURIComponent(voucherCode)}`;
        const res = await api.patch(url);
        return res.data;
    },

    // Lấy danh sách đơn cho nhà bếp
    getKitchenOrders: async () => {
        const res = await api.get('/orders/kitchen');
        return res.data;
    },

    // Cập nhật trạng thái món (PENDING, COOKING, FINISHED)
    updateItemStatus: async (itemId, status) => {
        const res = await api.patch(`/orders/items/${itemId}/status`, { status });
        return res.data;
    },

    getActiveOrders: async () => {
        const res = await api.get('/orders/active');
        return res.data;
    }
};