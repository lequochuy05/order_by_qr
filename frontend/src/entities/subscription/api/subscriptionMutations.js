import { useMutation } from '@tanstack/react-query';

import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { subscriptionService } from './subscriptionService.js';

export const invalidateSubscription = () => {
  queryClient.invalidateQueries({ queryKey: queryKeys.subscription.all });
  queryClient.invalidateQueries({ queryKey: queryKeys.settings.all });
};

export const useActivateTrialMutation = (options = {}) =>
  useMutation({
    mutationFn: () => subscriptionService.activateTrial(),
    onSuccess: (...args) => {
      invalidateSubscription();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useCreateSubscriptionCheckoutMutation = (options = {}) =>
  useMutation({
    mutationFn: (payload) => subscriptionService.createCheckout(payload),
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.subscription.invoices });
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useSyncSubscriptionInvoiceMutation = (options = {}) =>
  useMutation({
    mutationFn: (invoiceId) => subscriptionService.syncInvoice(invoiceId),
    onSuccess: (...args) => {
      invalidateSubscription();
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useCancelSubscriptionInvoiceMutation = (options = {}) =>
  useMutation({
    mutationFn: (invoiceId) => subscriptionService.cancelInvoice(invoiceId),
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.subscription.invoices });
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
