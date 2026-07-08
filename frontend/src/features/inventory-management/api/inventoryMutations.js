import { useMutation } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { queryClient } from '@shared/api/queryClient.js';
import { inventoryService } from '@entities/inventory/api/inventoryService.js';

const invalidateInventory = () => queryClient.invalidateQueries({ queryKey: queryKeys.inventory.all });

export const useCreateInventoryItemMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => inventoryService.createItem(data),
    onSuccess: (...args) => {
      invalidateInventory();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUpdateInventoryItemMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, data }) => inventoryService.updateItem(id, data),
    onSuccess: (...args) => {
      invalidateInventory();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useStockInMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, data }) => inventoryService.stockIn(id, data),
    onSuccess: (...args) => {
      invalidateInventory();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useAdjustStockMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, data }) => inventoryService.adjust(id, data),
    onSuccess: (...args) => {
      invalidateInventory();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUpdateRecipeMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ menuItemId, data }) => inventoryService.updateRecipe(menuItemId, data),
    onSuccess: (...args) => {
      invalidateInventory();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
