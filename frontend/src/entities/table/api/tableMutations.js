import { useMutation } from '@tanstack/react-query';

import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { tableService } from './tableService.js';

const invalidateTables = () => {
  queryClient.invalidateQueries({ queryKey: queryKeys.tables.all });
  queryClient.invalidateQueries({ queryKey: queryKeys.tables.board });
  queryClient.invalidateQueries({ queryKey: queryKeys.orders.all });
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

export const useUpdateTableStatusMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, status, version }) => tableService.updateStatus(id, status, version),
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

export const useMergeTablesMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => tableService.merge(data),
    onSuccess: (...args) => {
      invalidateTables();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useCancelMergeMutation = (options = {}) =>
  useMutation({
    mutationFn: (sessionId) => tableService.cancelMerge(sessionId),
    onSuccess: (...args) => {
      invalidateTables();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
