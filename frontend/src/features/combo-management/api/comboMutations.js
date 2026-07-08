import { useMutation } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { queryClient } from '@shared/api/queryClient.js';
import { comboService } from '@entities/combo/api/comboService.js';

const invalidateCombos = () => {
  queryClient.invalidateQueries({ queryKey: queryKeys.combos.all });
  queryClient.invalidateQueries({ queryKey: queryKeys.menu.all });
};

export const useCreateComboMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => comboService.create(data),
    onSuccess: (...args) => {
      invalidateCombos();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUpdateComboMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, data }) => comboService.update(id, data),
    onSuccess: (...args) => {
      invalidateCombos();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useDeleteComboMutation = (options = {}) =>
  useMutation({
    mutationFn: (id) => comboService.delete(id),
    onSuccess: (...args) => {
      invalidateCombos();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useToggleComboActiveMutation = (options = {}) =>
  useMutation({
    mutationFn: (id) => comboService.toggleActive(id),
    onSuccess: (...args) => {
      invalidateCombos();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
