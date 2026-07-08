import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { comboService } from '@entities/combo/api/comboService.js';

export const useComboPageQuery = (params = {}, options = {}) =>
  useQuery({
    queryKey: queryKeys.combos.list(params),
    queryFn: () => comboService.getPage(params),
    placeholderData: keepPreviousData,
    ...options,
  });

export const useComboDetailQuery = (id, options = {}) =>
  useQuery({
    queryKey: queryKeys.combos.detail(id),
    queryFn: () => comboService.getById(id),
    enabled: !!id,
    ...options,
  });
