import { useQuery } from '@tanstack/react-query';

import { queryKeys } from '@shared/api/queryKeys.js';
import { tableService } from './tableService.js';

export const useTableBoardQuery = (options = {}) =>
  useQuery({
    queryKey: queryKeys.tables.board,
    queryFn: () => tableService.getTableBoard(),
    ...options,
  });
