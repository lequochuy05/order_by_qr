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
  auth: createQueryKeys('auth'),
  
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
    revenue: (filters = {}) => [...queryKeys.analytics.all, 'revenue', filters],
    salesTrend: (filters = {}) => [...queryKeys.analytics.all, 'sales-trend', filters],
    topItems: (filters = {}) => [...queryKeys.analytics.all, 'top-items', filters],
    userPerformance: (filters = {}) => [...queryKeys.analytics.all, 'user-performance', filters],
  },

  vouchers: {
    all: ['vouchers'],
    lists: () => [...queryKeys.vouchers.all, 'list'],
    list: (filters = {}) => [...queryKeys.vouchers.lists(), filters],
    detail: (id) => [...queryKeys.vouchers.all, 'detail', id],
    validate: (code, subtotal) => [...queryKeys.vouchers.all, 'validate', code, subtotal],
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
    detail: (id) => ['tables', id],
  },
  
  menu: {
    all: ['menu'],
    customer: ['menu', 'customer'],
    detail: (id) => ['menu', id],
  },
  
  orders: {
    all: ['orders'],
    active: ['orders', 'active'],
    history: (filters = {}) => [
      'orders',
      'history',
      filters.page,
      filters.size,
      filters.from,
      filters.to,
      filters.status,
      filters.tableNumber,
    ],
    detail: (id) => ['orders', id],
  },

  categories: createQueryKeys('categories'),
  combos: createQueryKeys('combos'),
  kitchen: createQueryKeys('kitchen'),
  payments: createQueryKeys('payments'),
  inventory: createQueryKeys('inventory'),
  settings: createQueryKeys('settings'),
};
