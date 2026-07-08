import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { voucherService } from '@entities/voucher/api/voucherService.js';

export const useVoucherPageQuery = (params = {}, options = {}) =>
  useQuery({
    queryKey: queryKeys.vouchers.list(params),
    queryFn: () => voucherService.getPage(params, { force: false }),
    placeholderData: keepPreviousData,
    ...options,
  });
