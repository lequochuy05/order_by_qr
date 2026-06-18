import { useMutation } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { queryClient } from '@shared/api/queryClient.js';
import { orderService } from './orderService.js';

// Temporary invalidation hook for Sprint 3B. Will be consolidated in Sprint 3D.
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
    mutationFn: (itemId) => orderService.deleteOrderItem(itemId),
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
