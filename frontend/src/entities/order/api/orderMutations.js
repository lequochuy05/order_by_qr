import { useMutation } from '@tanstack/react-query';
import { publicMenuService } from '@entities/public-menu/api/publicMenuService.js';

export const createClientRequestId = () => {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID();
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
};

export const useSubmitOrderMutation = (options = {}) =>
  useMutation({
    mutationFn: (data) =>
      publicMenuService.createOrder({
        ...data,
        clientRequestId: data?.clientRequestId || createClientRequestId(),
      }),
    onSuccess: (...args) => {
      options.onSuccess?.(...args);
    },
    onError: (...args) => {
      options.onError?.(...args);
    },
  });
