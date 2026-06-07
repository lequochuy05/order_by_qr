import api from '@shared/api/httpClient.js';
import { formatBusinessDate } from '@shared/lib/businessTime.js';

export const statisticsService = {
    // 1. Chỉ lấy doanh thu
    getRevenue: async (from, to) => {
        const res = await api.get('/stats/revenue', {
            params: { from: formatBusinessDate(from), to: formatBusinessDate(to) }
        });
        return res || [];
    },

    // 2. Chỉ lấy hiệu suất nhân viên
    getEmployees: async (from, to) => {
        const res = await api.get('/stats/employees', {
            params: { from: formatBusinessDate(from), to: formatBusinessDate(to) }
        });
        return res || [];
    },

    // 3. Lấy đơn hàng (để tính toán món bán chạy)
    getOrders: async (from, to) => {
        const res = await api.get('/stats/orders', {
            params: { from: formatBusinessDate(from), to: formatBusinessDate(to) }
        });
        return res || [];
    },

    // 4. Lấy top món ăn bán chạy
    getTopDishes: async (from, to) => {
        const res = await api.get('/stats/top-dishes', {
            params: { from: formatBusinessDate(from), to: formatBusinessDate(to) }
        });
        return res || [];
    },

    // 5. Lấy xu hướng bán theo ngày
    getDishTrend: async (from, to) => {
        const res = await api.get('/stats/dish-trend', {
            params: { from: formatBusinessDate(from), to: formatBusinessDate(to) }
        });
        return res || [];
    },

    // 6. Lấy doanh thu thực tế 30 ngày và dự báo 7 ngày tới
    getRevenueForecast: async () => {
        const res = await api.get('/stats/forecast/revenue');
        return res || [];
    },

    // 7. Lấy top món dự báo bán chạy trong tuần tới
    getPopularDishesForecast: async () => {
        const res = await api.get('/stats/forecast/popular-dishes');
        return res || [];
    }
};
