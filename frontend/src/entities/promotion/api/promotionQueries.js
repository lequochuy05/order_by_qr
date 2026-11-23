import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { queryKeys } from '@shared/api/queryKeys.js';
import { promotionService } from './promotionService.js';

export const usePromotionPageQuery = (params = {}, options = {}) =>
  useQuery({
    queryKey: queryKeys.promotions.list(params),
    queryFn: () => promotionService.getPage(params, { force: false }),
    placeholderData: keepPreviousData,
    ...options,
  });

export const useActivePromotionsQuery = (options = {}) =>
  useQuery({
    queryKey: [...queryKeys.promotions.all, 'active'],
    queryFn: () => promotionService.getActive(),
    ...options,
  });
