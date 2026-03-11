import api from '../api';
const fmtDate = (d) => d instanceof Date ? d.toISOString().split('T')[0] : d;

export const statisticsService = {
    // 1. Chỉ lấy doanh thu
    getRevenue: async (from, to) => {
        const res = await api.get(`/stats/revenue?from=${fmtDate(from)}&to=${fmtDate(to)}`);
        return res.data || [];
    },

    // 2. Chỉ lấy hiệu suất nhân viên
    getEmployees: async (from, to) => {
        const res = await api.get(`/stats/employees?from=${fmtDate(from)}&to=${fmtDate(to)}`);
        return res.data || [];
    },

    // 3. Lấy đơn hàng (để tính toán món bán chạy)
    getOrders: async (from, to) => {
        const res = await api.get(`/stats/orders?from=${fmtDate(from)}&to=${fmtDate(to)}`);
        return res.data || [];
    }
};