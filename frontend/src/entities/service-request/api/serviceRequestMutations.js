import { useMutation } from '@tanstack/react-query';

import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { serviceRequestService } from './serviceRequestService.js';

const invalidateAllServiceRequests = () => {
  queryClient.invalidateQueries({ queryKey: queryKeys.serviceRequests.all });
};

export const useCreateServiceRequestMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ tableCode, payload }) =>
      serviceRequestService.createCustomerRequest(tableCode, payload),
    onSuccess: (data, variables, context) => {
      if (variables?.tableCode) {
        queryClient.invalidateQueries({
          queryKey: queryKeys.serviceRequests.customer(variables.tableCode),
        });
      }
      options.onSuccess?.(data, variables, context);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useCancelCustomerServiceRequestMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ tableCode, requestId, payload }) =>
      serviceRequestService.cancelCustomerRequest(tableCode, requestId, payload),
    onSuccess: (data, variables, context) => {
      if (variables?.tableCode) {
        queryClient.invalidateQueries({
          queryKey: queryKeys.serviceRequests.customer(variables.tableCode),
        });
      }
      options.onSuccess?.(data, variables, context);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useClaimServiceRequestMutation = (options = {}) =>
  useMutation({
    mutationFn: (id) => serviceRequestService.claimRequest(id),
    onSuccess: (...args) => {
      invalidateAllServiceRequests();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      invalidateAllServiceRequests();
      options.onError?.(...args);
    },
  });

export const useResolveServiceRequestMutation = (options = {}) =>
  useMutation({
    mutationFn: (id) => serviceRequestService.resolveRequest(id),
    onSuccess: (...args) => {
      invalidateAllServiceRequests();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      invalidateAllServiceRequests();
      options.onError?.(...args);
    },
  });

export const useCancelStaffServiceRequestMutation = (options = {}) =>
  useMutation({
    mutationFn: ({ id, reason }) => serviceRequestService.cancelStaffRequest(id, reason),
    onSuccess: (...args) => {
      invalidateAllServiceRequests();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      invalidateAllServiceRequests();
      options.onError?.(...args);
    },
  });
