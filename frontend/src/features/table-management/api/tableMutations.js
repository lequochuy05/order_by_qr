import { useMutation } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { queryClient } from '@shared/api/queryClient.js';
import { tableService } from './tableService.js';

// Central invalidation for tables (can be overridden by WebSocket Invalidator later)
const invalidateTables = () => {
  queryClient.invalidateQueries({ queryKey: queryKeys.tables.all });
  queryClient.invalidateQueries({ queryKey: queryKeys.tables.board });
};

export const useCreateTableMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => tableService.create(data),
    onSuccess: (...args) => {
      invalidateTables();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUpdateTableMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, data }) => tableService.update(id, data),
    onSuccess: (...args) => {
      invalidateTables();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useDeleteTableMutation = (options = {}) =>
  useMutation({
    mutationFn: (id) => tableService.delete(id),
    onSuccess: (...args) => {
      invalidateTables();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useRegenerateQrMutation = (options = {}) =>
  useMutation({
    mutationFn: (id) => tableService.regenerateQr(id),
    onSuccess: (...args) => {
      invalidateTables();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
