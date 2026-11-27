import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { queryKeys } from '@shared/api/queryKeys.js';
import { orderService } from './orderService.js';

export const useOrdersHistoryQuery = (filters = {}, options = {}) =>
  useQuery({
    queryKey: queryKeys.orders.history(filters),
    queryFn: () => orderService.getOrderHistory(filters),
    placeholderData: keepPreviousData,
    ...options,
  });

export const useOrderAnalyticsQuery = (filters = {}, options = {}) =>
  useQuery({
    queryKey: [
      ...queryKeys.orders.all,
      'analytics',
      filters.from,
      filters.to,
      filters.status,
      filters.tableNumber,
      filters.orderId,
    ],
    queryFn: () => orderService.getOrderAnalytics(filters),
    ...options,
  });
