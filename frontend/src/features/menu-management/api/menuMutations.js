import { useMutation } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { queryClient } from '@shared/api/queryClient.js';
import { menuItemService } from './menuService.js';

const invalidateMenu = () =>
  queryClient.invalidateQueries({ queryKey: queryKeys.menu.all });

export const useCreateMenuMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => menuItemService.create(data),
    onSuccess: (...args) => {
      invalidateMenu();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUpdateMenuMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, data }) => menuItemService.update(id, data),
    onSuccess: (...args) => {
      invalidateMenu();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useDeleteMenuMutation = (options = {}) =>
  useMutation({
    mutationFn: (id) => menuItemService.delete(id),
    onSuccess: (...args) => {
      invalidateMenu();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUploadMenuImageMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, file }) => menuItemService.uploadImage(id, file),
    onSuccess: (...args) => {
      invalidateMenu();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
