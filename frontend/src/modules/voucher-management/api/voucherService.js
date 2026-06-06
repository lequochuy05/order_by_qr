import api from '@shared/api/httpClient.js';

export const voucherService = {
    // Lấy toàn bộ voucher
    getAll: async () => {
        const res = await api.get('/vouchers');
        return res;
    },

    // Tạo mới voucher
    create: async (data) => {
        const res = await api.post('/vouchers', data);
        return res;
    },

    // Cập nhật voucher
    update: async (id, data) => {
        const res = await api.put(`/vouchers/${id}`, data);
        return res;
    },

    // Xóa voucher
    delete: async (id) => {
        await api.delete(`/vouchers/${id}`);
    
    }
};