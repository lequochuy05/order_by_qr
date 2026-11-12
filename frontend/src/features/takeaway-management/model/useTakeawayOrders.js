import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { takeawayService } from '@entities/order';

export const useTakeawayOrders = (options = {}) =>
  useQuery({
    queryKey: queryKeys.orders.activeTakeaway,
    queryFn: () => takeawayService.getActiveOrders(),
    refetchInterval: 15_000,
    ...options,
  });
