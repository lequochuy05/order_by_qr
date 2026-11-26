import api from '@shared/api/httpClient.js';
import { formatBusinessDate } from '@shared/lib/businessTime.js';
import { stableStringify } from '@shared/lib/cacheUtils.js';
import { toNumber } from '@shared/lib/formatters.js';
import { emptyPage } from '@shared/lib/pagination.js';

// In-flight deduplication only; React Query owns caching and WebSocket invalidation.
const analyticsRequests = new Map();

export const clearAnalyticsCache = () => {
  analyticsRequests.clear();
};

const getDedupedAnalytics = async (key, requestFactory, fallback) => {
  if (!analyticsRequests.has(key)) {
    analyticsRequests.set(
      key,
      requestFactory()
        .then((res) => res ?? fallback)
        .finally(() => {
          analyticsRequests.delete(key);
        }),
    );
  }
  return analyticsRequests.get(key);
};

const rangeParams = (from, to) => ({
  from: formatBusinessDate(from),
  to: formatBusinessDate(to),
});

const pagedFallback = (page, size) => emptyPage(page, size);

const translateApiLabel = (value) =>
  ({ Staff: 'Nhân viên', Uncategorized: 'Chưa phân loại' })[value] || value;

const normalizeRevenuePoint = (point = {}) => ({
  ...point,
  bucket: point.bucket ?? point.date,
  orders: point.orders ?? point.orderCount ?? 0,
  revenue: toNumber(point.revenue),
});

const normalizeUser = (user = {}) => ({
  ...user,
  fullName: translateApiLabel(user.fullName),
  orders: user.orders ?? user.orderCount ?? 0,
  revenue: toNumber(user.revenue),
});

const normalizeOrderDetail = (order = {}) => ({
  ...order,
  id: order.id ?? order.orderId,
  empName: order.empName ?? order.userName,
  finalAmount: toNumber(order.finalAmount),
});

const normalizeTopItem = (item = {}) => ({
  ...item,
  id: item.id ?? item.menuItemId,
  name: item.name ?? item.itemName ?? '',
  category: translateApiLabel(item.category ?? item.categoryName) || 'Khác',
  img: item.img ?? item.imageUrl,
  totalQty: item.totalQty ?? item.quantitySold ?? 0,
  totalRevenue: toNumber(item.totalRevenue ?? item.revenue),
});

const normalizeSalesTrendPoint = (point = {}) => ({
  ...point,
  bucket: point.bucket ?? point.date,
  totalQty: point.totalQty ?? point.quantitySold ?? 0,
});

const normalizeRevenueForecastPoint = (point = {}) => ({
  ...point,
  actual: point.actual ?? point.actualRevenue,
  forecast: point.forecast ?? point.forecastRevenue,
});

const normalizePopularForecastItem = (item = {}) => ({
  ...item,
  id: item.id ?? item.menuItemId,
  name: item.name ?? item.itemName ?? '',
  category: translateApiLabel(item.category ?? item.categoryName) || 'Chưa phân loại',
  estimatedQty: item.estimatedQty ?? item.estimatedQuantity ?? 0,
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
    todayRevenue: toNumber(dashboard.todayRevenue),
    averageOrderValue: toNumber(dashboard.averageOrderValue ?? dashboard.avgOrderValue),
    todayAvgOrderValue: toNumber(dashboard.todayAvgOrderValue),
    todayOrders: {
      ...todayOrders,
      total: todayOrders.total ?? todayOrders.totalOrders ?? 0,
      completed: todayOrders.completed ?? todayOrders.completedOrders ?? 0,
    },
    tables: {
      ...tables,
      total: tables.total ?? tables.totalTables ?? 0,
      occupied: tables.occupied ?? tables.occupiedTables ?? 0,
    },
    recentOrders: Array.isArray(dashboard.recentOrders)
      ? dashboard.recentOrders.map(normalizeOrderDetail)
      : [],
    topDishes: Array.isArray(dashboard.topDishes) ? dashboard.topDishes.map(normalizeTopItem) : topItems,
    topItems,
    salesTrend: Array.isArray(dashboard.salesTrend)
      ? dashboard.salesTrend.map(normalizeSalesTrendPoint)
      : [],
    revenue: Array.isArray(dashboard.revenue) ? dashboard.revenue.map(normalizeRevenuePoint) : [],
    revenueForecast: Array.isArray(dashboard.revenueForecast)
      ? dashboard.revenueForecast.map(normalizeRevenueForecastPoint)
      : [],
    popularDishesForecast: Array.isArray(dashboard.popularDishesForecast)
      ? dashboard.popularDishesForecast.map(normalizePopularForecastItem)
      : popularItemsForecast,
    popularItemsForecast,
  };
};

export const analyticsService = {
  clearCache: clearAnalyticsCache,
  getRevenue: async (from, to) => {
    const params = rangeParams(from, to);
    const data = await getDedupedAnalytics(
      `revenue:${stableStringify(params)}`,
      () => api.get('/analytics/revenue', { params }),
      [],
    );
    return data.map(normalizeRevenuePoint);
  },
  getUsers: async (from, to) => {
    const params = rangeParams(from, to);
    const data = await getDedupedAnalytics(
      `users:${stableStringify(params)}`,
      () => api.get('/analytics/users', { params }),
      [],
    );
    return data.map(normalizeUser);
  },
  getOrders: async (from, to, options = {}) => {
    const page = Number.isInteger(options.page) && options.page >= 0 ? options.page : 0;
    const size = Number.isInteger(options.size) && options.size > 0 ? options.size : 10;
    const params = { ...rangeParams(from, to), page, size };
    const pageData = await getDedupedAnalytics(
      `orders:${stableStringify(params)}`,
      () => api.get('/analytics/orders', { params }),
      pagedFallback(page, size),
    );
    return {
      ...pageData,
      content: Array.isArray(pageData.content) ? pageData.content.map(normalizeOrderDetail) : [],
    };
  },
  getTopDishes: async (from, to) => {
    const params = rangeParams(from, to);
    const data = await getDedupedAnalytics(
      `top-dishes:${stableStringify(params)}`,
      () => api.get('/analytics/top-items', { params }),
      [],
    );
    return data.map(normalizeTopItem);
  },
  getDishTrend: async (from, to) => {
    const params = rangeParams(from, to);
    const data = await getDedupedAnalytics(
      `dish-trend:${stableStringify(params)}`,
      () => api.get('/analytics/sales-trend', { params }),
      [],
    );
    return data.map(normalizeSalesTrendPoint);
  },
  getRevenueForecast: async () => {
    const data = await getDedupedAnalytics(
      'forecast:revenue',
      () => api.get('/analytics/forecast/revenue'),
      [],
    );
    return data.map(normalizeRevenueForecastPoint);
  },
  getPopularDishesForecast: async () => {
    const data = await getDedupedAnalytics(
      'forecast:popular-dishes',
      () => api.get('/analytics/forecast/popular-items'),
      [],
    );
    return data.map(normalizePopularForecastItem);
  },
  getDashboardSummary: async (from, to, options = {}) => {
    const params = rangeParams(from, to);
    const data = await getDedupedAnalytics(
      `dashboard:${stableStringify(params)}`,
      () => api.get('/analytics/dashboard', { params, signal: options.signal }),
      {},
    );
    return normalizeDashboardSummary(data || {});
  },
  getAiExecutiveSummary: async ({ from, to, language = 'vi' } = {}) => {
    const body = {
      from: from ? formatBusinessDate(from) : null,
      to: to ? formatBusinessDate(to) : null,
      language: language === 'en' ? 'en' : 'vi',
    };
    return api.post('/ai/insights/summary', body);
  },
};
