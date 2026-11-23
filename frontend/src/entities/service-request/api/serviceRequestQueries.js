import { useQuery } from '@tanstack/react-query';

import { queryKeys } from '@shared/api/queryKeys.js';
import { serviceRequestService } from './serviceRequestService.js';

export const useCustomerServiceRequestsQuery = (tableCode, sessionToken, options = {}) =>
  useQuery({
    queryKey: queryKeys.serviceRequests.customer(tableCode),
    queryFn: () => serviceRequestService.getCustomerRequests(tableCode, sessionToken),
    enabled: Boolean(tableCode && sessionToken) && (options.enabled ?? true),
    retry: false,
    staleTime: 10 * 1000,
    ...options,
  });

export const useServiceRequestsQueueQuery = (
  filters = { status: 'OPEN', mineOnly: false, tableId: null },
  options = {},
) => {
  const normalizedFilters = {
    status: filters?.status || 'OPEN',
    mineOnly: Boolean(filters?.mineOnly),
    tableId: filters?.tableId ?? null,
  };

  return useQuery({
    queryKey: queryKeys.serviceRequests.queue(normalizedFilters),
    queryFn: () => serviceRequestService.getStaffQueue(normalizedFilters),
    staleTime: 10 * 1000,
    ...options,
  });
};
