import { useMutation } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { queryClient } from '@shared/api/queryClient.js';
import { categoryService } from './categoryService.js';

const invalidateCategories = () =>
  queryClient.invalidateQueries({ queryKey: queryKeys.categories.all });

export const useCreateCategoryMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => categoryService.create(data),
    onSuccess: (...args) => {
      invalidateCategories();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUpdateCategoryMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, data }) => categoryService.update(id, data),
    onSuccess: (...args) => {
      invalidateCategories();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useDeleteCategoryMutation = (options = {}) =>
  useMutation({
    mutationFn: (id) => categoryService.delete(id),
    onSuccess: (...args) => {
      invalidateCategories();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUploadCategoryImageMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, file }) => categoryService.uploadImage(id, file),
    onSuccess: (...args) => {
      invalidateCategories();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
