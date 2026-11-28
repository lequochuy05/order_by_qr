import { useQuery } from '@tanstack/react-query';

import { queryKeys } from '@shared/api/queryKeys.js';
import { subscriptionService } from './subscriptionService.js';

export const useCurrentSubscriptionQuery = (options = {}) =>
  useQuery({
    queryKey: queryKeys.subscription.current,
    queryFn: () => subscriptionService.getCurrent(),
    staleTime: 30 * 1000,
    ...options,
  });

export const useSubscriptionPlansQuery = (options = {}) =>
  useQuery({
    queryKey: queryKeys.subscription.plans,
    queryFn: () => subscriptionService.getPlans(),
    staleTime: 5 * 60 * 1000,
    ...options,
  });

export const useSubscriptionInvoicesQuery = (options = {}) =>
  useQuery({
    queryKey: queryKeys.subscription.invoices,
    queryFn: () => subscriptionService.getInvoices(),
    ...options,
  });
