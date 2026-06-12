import api from '@shared/api/httpClient.js';
import { formatBusinessDate } from '@shared/lib/businessTime.js';

const STATS_CACHE_MS = 5_000;

const statsRequests = new Map();
const statsCache = new Map();

const stableStringify = (value) => {
    if (Array.isArray(value)) {
        return `[${value.map(stableStringify).join(',')}]`;
    }
    if (value && typeof value === 'object') {
        return `{${Object.keys(value).sort().map(key => `${JSON.stringify(key)}:${stableStringify(value[key])}`).join(',')}}`;
    }
    return JSON.stringify(value);
};

export const clearStatisticsCache = () => {
    statsRequests.clear();
    statsCache.clear();
};

const getCachedStats = async (key, requestFactory, fallback, { force = false, cacheMs = STATS_CACHE_MS } = {}) => {
    const cached = statsCache.get(key);
    if (!force && cached && cached.expiresAt > Date.now()) {
        return cached.data;
    }

    if (!statsRequests.has(key)) {
        statsRequests.set(
            key,
            requestFactory()
                .then((res) => {
                    const data = res ?? fallback;
                    statsCache.set(key, {
                        data,
                        expiresAt: Date.now() + cacheMs
                    });
                    return data;
                })
                .finally(() => {
                    statsRequests.delete(key);
                })
        );
    }

    return statsRequests.get(key);
};

const rangeParams = (from, to) => ({
    from: formatBusinessDate(from),
    to: formatBusinessDate(to)
});

const pagedFallback = (page, size) => ({
    content: [],
    number: page,
    size,
    totalElements: 0,
    totalPages: 0
});

const toNumber = (value) => Number(value ?? 0);

const normalizeRevenuePoint = (point = {}) => ({
    ...point,
    bucket: point.bucket ?? point.date,
    orders: point.orders ?? point.orderCount ?? 0,
    revenue: toNumber(point.revenue)
});

const normalizeEmployee = (employee = {}) => ({
    ...employee,
    orders: employee.orders ?? employee.orderCount ?? 0,
    revenue: toNumber(employee.revenue)
});

const normalizeOrderDetail = (order = {}) => ({
    ...order,
    id: order.id ?? order.orderId,
    empName: order.empName ?? order.staffName,
    finalAmount: toNumber(order.finalAmount)
});

const normalizeTopItem = (item = {}) => ({
    ...item,
    id: item.id ?? item.menuItemId,
    name: item.name ?? item.itemName ?? '',
    category: item.category ?? item.categoryName ?? 'Khác',
    img: item.img ?? item.imageUrl,
    totalQty: item.totalQty ?? item.quantitySold ?? 0,
    totalRevenue: toNumber(item.totalRevenue ?? item.revenue)
});

const normalizeSalesTrendPoint = (point = {}) => ({
    ...point,
    bucket: point.bucket ?? point.date,
    totalQty: point.totalQty ?? point.quantitySold ?? 0
});

const normalizeRevenueForecastPoint = (point = {}) => ({
    ...point,
    actual: point.actual ?? point.actualRevenue,
    forecast: point.forecast ?? point.forecastRevenue
});

const normalizePopularForecastItem = (item = {}) => ({
    ...item,
    id: item.id ?? item.menuItemId,
    name: item.name ?? item.itemName ?? '',
    category: item.category ?? item.categoryName ?? 'Chưa phân loại',
    estimatedQty: item.estimatedQty ?? item.estimatedQuantity ?? 0
});

const normalizeDashboardSummary = (dashboard = {}) => {
    const todayOrders = dashboard.todayOrders || {};
    const tables = dashboard.tables || {};
    const topItems = Array.isArray(dashboard.topItems) ? dashboard.topItems.map(normalizeTopItem) : [];

    const popularItemsForecast = Array.isArray(dashboard.popularItemsForecast)
        ? dashboard.popularItemsForecast.map(normalizePopularForecastItem)
        : [];

    return {
        ...dashboard,
        todayRevenue: toNumber(dashboard.todayRevenue ?? dashboard.totalRevenue),
        todayOrders: {
            ...todayOrders,
            total: todayOrders.total ?? todayOrders.totalOrders ?? 0,
            completed: todayOrders.completed ?? todayOrders.completedOrders ?? 0
        },
        tables: {
            ...tables,
            total: tables.total ?? tables.totalTables ?? 0,
            occupied: tables.occupied ?? tables.occupiedTables ?? 0
        },
        recentOrders: Array.isArray(dashboard.recentOrders)
            ? dashboard.recentOrders.map(normalizeOrderDetail)
            : [],
        topDishes: Array.isArray(dashboard.topDishes)
            ? dashboard.topDishes.map(normalizeTopItem)
            : topItems,
        topItems,
        salesTrend: Array.isArray(dashboard.salesTrend)
            ? dashboard.salesTrend.map(normalizeSalesTrendPoint)
            : [],
        revenue: Array.isArray(dashboard.revenue)
            ? dashboard.revenue.map(normalizeRevenuePoint)
            : [],
        revenueForecast: Array.isArray(dashboard.revenueForecast)
            ? dashboard.revenueForecast.map(normalizeRevenueForecastPoint)
            : [],
        popularDishesForecast: Array.isArray(dashboard.popularDishesForecast)
            ? dashboard.popularDishesForecast.map(normalizePopularForecastItem)
            : popularItemsForecast,
        popularItemsForecast
    };
};

export const statisticsService = {
    clearCache: clearStatisticsCache,

    // 1. Chỉ lấy doanh thu
    getRevenue: async (from, to, options = {}) => {
        const params = rangeParams(from, to);
        const data = await getCachedStats(
            `revenue:${stableStringify(params)}`,
            () => api.get('/analytics/revenue', { params }),
            [],
            options
        );
        return data.map(normalizeRevenuePoint);
    },

    // 2. Chỉ lấy hiệu suất nhân viên
    getEmployees: async (from, to, options = {}) => {
        const params = rangeParams(from, to);
        const data = await getCachedStats(
            `employees:${stableStringify(params)}`,
            () => api.get('/analytics/employees', { params }),
            [],
            options
        );
        return data.map(normalizeEmployee);
    },

    // 3. Lấy đơn hàng chi tiết theo trang cho báo cáo doanh thu
    getOrders: async (from, to, options = {}) => {
        const page = Number.isInteger(options.page) && options.page >= 0 ? options.page : 0;
        const size = Number.isInteger(options.size) && options.size > 0 ? options.size : 10;
        const params = {
            ...rangeParams(from, to),
            page,
            size
        };

        const pageData = await getCachedStats(
            `orders:${stableStringify(params)}`,
            () => api.get('/analytics/orders', { params }),
            pagedFallback(page, size),
            options
        );
        return {
            ...pageData,
            content: Array.isArray(pageData.content)
                ? pageData.content.map(normalizeOrderDetail)
                : []
        };
    },

    // 4. Lấy top món ăn bán chạy
    getTopDishes: async (from, to, options = {}) => {
        const params = rangeParams(from, to);
        const data = await getCachedStats(
            `top-dishes:${stableStringify(params)}`,
            () => api.get('/analytics/top-items', { params }),
            [],
            options
        );
        return data.map(normalizeTopItem);
    },

    // 5. Lấy xu hướng bán theo ngày
    getDishTrend: async (from, to, options = {}) => {
        const params = rangeParams(from, to);
        const data = await getCachedStats(
            `dish-trend:${stableStringify(params)}`,
            () => api.get('/analytics/sales-trend', { params }),
            [],
            options
        );
        return data.map(normalizeSalesTrendPoint);
    },

    // 6. Lấy doanh thu thực tế 30 ngày và dự báo 7 ngày tới
    getRevenueForecast: async (options = {}) => {
        const data = await getCachedStats(
            'forecast:revenue',
            () => api.get('/analytics/forecast/revenue'),
            [],
            options
        );
        return data.map(normalizeRevenueForecastPoint);
    },

    // 7. Lấy top món dự báo bán chạy trong tuần tới
    getPopularDishesForecast: async (options = {}) => {
        const data = await getCachedStats(
            'forecast:popular-dishes',
            () => api.get('/analytics/forecast/popular-items'),
            [],
            options
        );
        return data.map(normalizePopularForecastItem);
    },

    // 8. Payload tổng hợp cho dashboard
    getDashboardSummary: async (from, to, options = {}) => {
        const res = await api.get('/analytics/dashboard', {
            params: rangeParams(from, to),
            signal: options.signal
        });
        return normalizeDashboardSummary(res || {});
    }
};
