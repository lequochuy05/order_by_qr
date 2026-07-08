import { useMutation } from '@tanstack/react-query';
import { queryKeys } from '@shared/api/queryKeys.js';
import { queryClient } from '@shared/api/queryClient.js';
import { voucherService } from '@entities/voucher/api/voucherService.js';

const invalidateVouchers = () =>
  queryClient.invalidateQueries({ queryKey: queryKeys.vouchers.all });

export const useCreateVoucherMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) => voucherService.create(data),
    onSuccess: (...args) => {
      invalidateVouchers();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useUpdateVoucherMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, data }) => voucherService.update(id, data),
    onSuccess: (...args) => {
      invalidateVouchers();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useDeleteVoucherMutation = (options = {}) =>
  useMutation({
    mutationFn: (id) => voucherService.delete(id),
    onSuccess: (...args) => {
      invalidateVouchers();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
