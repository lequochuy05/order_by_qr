import api from '../api';

export const voucherService = {
    // Lấy toàn bộ voucher
    getAll: async () => {
        const res = await api.get('/vouchers');
        return res.data;
    },

    // Tạo mới voucher
    create: async (data) => {
        const res = await api.post('/vouchers', data);
        return res.data;
    },

    // Cập nhật voucher
    update: async (id, data) => {
        const res = await api.put(`/vouchers/${id}`, data);
        return res.data;
    },

    // Xóa voucher
    delete: async (id) => {
        await api.delete(`/vouchers/${id}`);
    
    }
};