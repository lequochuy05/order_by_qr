/**
 * Query Key Factory for the entire QROS domain.
 *
 * Every domain is declared upfront so WebSocket event handlers
 * can reference keys correctly without hardcoding strings.
 */

const createQueryKeys = (entity) => ({
  all: [entity],
  lists: () => [[entity], 'list'],
  list: (filters = {}) => [[entity, 'list'], filters],
  detail: (id) => [[entity], 'detail', id],
});

export const queryKeys = {
  users: {
    all: ['users'],
    lists: () => [...queryKeys.users.all, 'list'],
    list: (filters = {}) => [...queryKeys.users.lists(), filters],
    detail: (id) => [...queryKeys.users.all, 'detail', id],
    profile: () => [...queryKeys.users.all, 'profile'],
  },

  analytics: {
    all: ['analytics'],
    dashboard: (filters = {}) => [...queryKeys.analytics.all, 'dashboard', filters],
    revenue: (from, to) => [...queryKeys.analytics.all, 'revenue', from, to],
    orders: (from, to, page, size) => [...queryKeys.analytics.all, 'orders', from, to, page, size],
    staff: (from, to) => [...queryKeys.analytics.all, 'staff', from, to],
    topDishes: (from, to) => [...queryKeys.analytics.all, 'top-dishes', from, to],
    dishTrend: (from, to) => [...queryKeys.analytics.all, 'dish-trend', from, to],
  },

  vouchers: {
    all: ['vouchers'],
    lists: () => [...queryKeys.vouchers.all, 'list'],
    list: (filters = {}) => [...queryKeys.vouchers.lists(), filters],
    detail: (id) => [...queryKeys.vouchers.all, 'detail', id],
  },

  promotions: {
    all: ['promotions'],
    lists: () => [...queryKeys.promotions.all, 'list'],
    list: (filters = {}) => [...queryKeys.promotions.lists(), filters],
    detail: (id) => [...queryKeys.promotions.all, 'detail', id],
  },

  tables: {
    all: ['tables'],
    board: ['tables', 'board'],
  },

  serviceRequests: {
    all: ['service-requests'],
    queue: (filters = {}) => ['service-requests', 'queue', filters],
    customer: (tableCode) => ['service-requests', 'customer', tableCode],
  },

  menu: {
    all: ['menu'],
    customer: ['menu', 'customer'],
  },

  orders: {
    all: ['orders'],
    active: ['orders', 'active'],
    activeTakeaway: ['orders', 'active-takeaway'],
    kitchen: ['orders', 'kitchen'],
    history: (filters = null) =>
      filters && Object.keys(filters).length > 0
        ? [
            'orders',
            'history',
            filters.page,
            filters.size,
            filters.from,
            filters.to,
            filters.status,
            filters.tableNumber,
            filters.orderId,
          ]
        : ['orders', 'history'],
  },

  categories: createQueryKeys('categories'),
  combos: createQueryKeys('combos'),
  inventory: createQueryKeys('inventory'),
  settings: createQueryKeys('settings'),
  subscription: {
    all: ['subscription'],
    current: ['subscription', 'current'],
    plans: ['subscription', 'plans'],
    invoices: ['subscription', 'invoices'],
  },
};
