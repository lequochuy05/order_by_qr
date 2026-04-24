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
    },

    // 4. Lấy top món ăn bán chạy
    getTopDishes: async (from, to) => {
        const res = await api.get(`/stats/top-dishes?from=${fmtDate(from)}&to=${fmtDate(to)}`);
        return res.data || [];
    },

    // 5. Lấy xu hướng bán theo ngày
    getDishTrend: async (from, to) => {
        const res = await api.get(`/stats/dish-trend?from=${fmtDate(from)}&to=${fmtDate(to)}`);
        return res.data || [];
    }
};