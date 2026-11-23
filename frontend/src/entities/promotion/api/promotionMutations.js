import { useMutation } from '@tanstack/react-query';

import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { promotionService } from './promotionService.js';

const invalidatePromotions = () =>
  queryClient.invalidateQueries({ queryKey: queryKeys.promotions.all });

export const useCreatePromotionMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => promotionService.create(data),
    onSuccess: (...args) => {
      invalidatePromotions();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUpdatePromotionMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, data }) => promotionService.update(id, data),
    onSuccess: (...args) => {
      invalidatePromotions();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useDeletePromotionMutation = (options = {}) =>
  useMutation({
    mutationFn: (id) => promotionService.delete(id),
    onSuccess: (...args) => {
      invalidatePromotions();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
