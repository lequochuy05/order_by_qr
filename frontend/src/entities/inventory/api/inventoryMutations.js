import { useMutation } from '@tanstack/react-query';

import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { inventoryService } from './inventoryService.js';

const invalidateInventory = () =>
  queryClient.invalidateQueries({ queryKey: queryKeys.inventory.all });

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

export const useBatchStockInMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => inventoryService.batchStockIn(data),
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
