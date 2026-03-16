import api from '../api';

export const orderService = {
    getCurrentOrder: async(tableId) => {
        try {
            const res = await api.get(`/orders/table/${tableId}/current`);
            return res.status === 200 ? res.data : null;
        } catch (error) {
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
        const res = await api.put(`/orders/items/${itemId}`, data);
        return res.data;
    },

    // Hủy món
    deleteOrderItem: async (itemId) => {
        await api.delete(`/orders/items/${itemId}`);
    },

    // Đánh dấu đã phục vụ (bếp xong)
    markItemPrepared: async (itemId) => {
        await api.put(`/orders/items/${itemId}/prepared`);
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
        const res = await api.put(url);
        return res.data;
    },

    // Lấy danh sách đơn cho nhà bếp
    getKitchenOrders: async () => {
        const res = await api.get('/orders/kitchen');
        return res.data;
    },

    // Cập nhật trạng thái món (PENDING, COOKING, FINISHED)
    updateItemStatus: async (itemId, status) => {
        const res = await api.put(`/orders/items/${itemId}/status`, { status });
        return res.data;
    }
};