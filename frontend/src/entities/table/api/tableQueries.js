import { useQuery } from '@tanstack/react-query';

import { orderService } from '@entities/order';
import { queryKeys } from '@shared/api/queryKeys.js';
import { tableService } from './tableService.js';

export const useTableBoardQuery = (options = {}) =>
  useQuery({
    queryKey: queryKeys.tables.board,
    queryFn: () => orderService.getTableBoard(),
    ...options,
  });

export const useAllTablesQuery = (options = {}) =>
  useQuery({
    queryKey: queryKeys.tables.all,
    queryFn: () => tableService.getAll(),
    ...options,
  });

export const useTableDetailQuery = (id, options = {}) =>
  useQuery({
    queryKey: queryKeys.tables.detail(id),
    queryFn: () => tableService.getById(id),
    enabled: !!id,
    ...options,
  });
