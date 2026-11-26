import { useMutation } from '@tanstack/react-query';
import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { createIdempotencyKey } from '@shared/lib/idempotency.js';
import { orderService } from './orderService.js';

export const createClientRequestId = () => createIdempotencyKey();

export const useSubmitOrderMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) =>
      orderService.createPublicOrder({
        ...data,
        clientRequestId: data?.clientRequestId || createClientRequestId(),
      }),
    onSuccess: (...args) => {
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

const invalidateOrders = () => {
  queryClient.invalidateQueries({ queryKey: queryKeys.orders.all });
  queryClient.invalidateQueries({ queryKey: queryKeys.tables.all });
};

export const useReconcileOrderMutation = (options = {}) =>
  useMutation({
    mutationFn: (orderId) => orderService.reconcileOrder(orderId),
    onSuccess: (...args) => {
      invalidateOrders();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
