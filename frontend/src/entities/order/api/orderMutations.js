import { useMutation } from '@tanstack/react-query';
import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { publicMenuService } from '@entities/public-menu/api/publicMenuService.js';
import { orderService } from './orderService.js';

export const createClientRequestId = () => {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID();
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
};

export const useSubmitOrderMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) =>
      publicMenuService.createOrder({
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

export const useAddItemsToOrderMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => orderService.addItemsToOrder(data),
    onSuccess: (...args) => {
      invalidateOrders();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUpdateOrderItemMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ itemId, data }) => orderService.updateOrderItem(itemId, data),
    onSuccess: (...args) => {
      invalidateOrders();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useDeleteOrderItemMutation = (options = {}) =>
  useMutation({
    mutationFn: (param) => {
      const itemId = typeof param === 'object' && param !== null ? param.itemId : param;
      const reason = typeof param === 'object' && param !== null ? param.reason : undefined;
      return orderService.deleteOrderItem(itemId, reason);
    },
    onSuccess: (...args) => {
      invalidateOrders();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useCancelOrderMutation = (options = {}) =>
  useMutation({
    mutationFn: (param) => {
      const orderId = typeof param === 'object' && param !== null ? param.orderId : param;
      const reason = typeof param === 'object' && param !== null ? param.reason : undefined;
      return orderService.cancelOrder(orderId, reason);
    },
    onSuccess: (...args) => {
      invalidateOrders();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useMarkItemPreparedMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ itemId, userId }) => orderService.markItemPrepared(itemId, userId),
    onSuccess: (...args) => {
      invalidateOrders();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUpdateItemStatusMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ itemId, status, userId }) =>
      orderService.updateItemStatus(itemId, status, userId),
    onSuccess: (...args) => {
      invalidateOrders();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

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
