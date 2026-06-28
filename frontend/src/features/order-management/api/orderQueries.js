import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { orderService } from '@entities/order/api/orderService.js';

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
      'orders',
      'analytics',
      filters.from,
      filters.to,
      filters.status,
      filters.tableNumber,
    ],
    queryFn: () => orderService.getOrderAnalytics(filters),
    ...options,
  });

export const useActiveOrdersQuery = (options = {}) =>
  useQuery({
    queryKey: queryKeys.orders.active,
    queryFn: () => orderService.getActiveOrders(),
    ...options,
  });

export const useCurrentOrderQuery = (tableId, options = {}) =>
  useQuery({
    queryKey: queryKeys.orders.detail(`current-${tableId}`),
    queryFn: () => orderService.getCurrentOrder(tableId),
    enabled: !!tableId,
    ...options,
  });
