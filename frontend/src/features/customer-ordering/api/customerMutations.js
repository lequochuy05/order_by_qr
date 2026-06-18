import { useMutation } from '@tanstack/react-query';
import { menuService } from './menuService.js';

export const createClientRequestId = () => {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID();
  }

  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
};

export const useSubmitOrderMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) =>
      menuService.createOrder({
        ...data,
        clientRequestId: data?.clientRequestId || createClientRequestId(),
      }),
    onSuccess: (...args) => {
      // Typically we invalidate table session after submit
      // options.onSuccess will receive the arguments
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });

export const useStartTableSessionMutation = (options = {}) =>
  useMutation({
    mutationFn: (tableCode) => menuService.startSession(tableCode),
    onSuccess: (...args) => {
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
