import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { orderService } from '@features/order-management/api/orderService.js';
import { tableService } from './tableService.js';

/**
 * Fetches the aggregated table board which includes both all tables and active orders.
 * Key: queryKeys.tables.board
 */
export const useTableBoardQuery = (options = {}) =>
  useQuery({
    queryKey: queryKeys.tables.board,
    queryFn: () => orderService.getTableBoard(),
    ...options,
  });

/**
 * Fetches all tables without active orders data.
 */
export const useAllTablesQuery = (options = {}) =>
  useQuery({
    queryKey: queryKeys.tables.all,
    queryFn: () => tableService.getAll(),
    ...options,
  });

/**
 * Fetches a single table's details.
 */
export const useTableDetailQuery = (id, options = {}) =>
  useQuery({
    queryKey: queryKeys.tables.detail(id),
    queryFn: () => tableService.getById(id),
    enabled: !!id,
    ...options,
  });
