import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { queryKeys } from '@shared/api/queryKeys.js';
import { comboService } from './comboService.js';

export const useComboPageQuery = (params = {}, options = {}) =>
  useQuery({
    queryKey: [...queryKeys.combos.all, 'page', params],
    queryFn: () => comboService.getPage(params),
    placeholderData: keepPreviousData,
    ...options,
  });

export const useComboDetailQuery = (id, options = {}) =>
  useQuery({
    queryKey: [...queryKeys.combos.all, 'detail', id],
    queryFn: () => comboService.getById(id),
    enabled: !!id,
    ...options,
  });
