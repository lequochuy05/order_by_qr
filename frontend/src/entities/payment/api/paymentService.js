import api from '@shared/api/httpClient.js';
import { createIdempotencyKey } from '@shared/lib/idempotency.js';
import { createPaymentWithRetry } from '../lib/paymentRetry.js';

export const paymentService = {
  createPayment: async (
    orderId,
    paymentMethod,
    voucherCode = null,
    { cashReceived = null, changeAmount = null, confirmUnfinishedItems = false } = {},
  ) => {
    const idempotencyKey = createIdempotencyKey();
    return createPaymentWithRetry({
      post: (url, payload) => api.post(url, payload),
      orderId,
      paymentMethod,
      voucherCode,
      idempotencyKey,
      cashReceived,
      changeAmount,
      confirmUnfinishedItems,
    });
  },
  cancelPaymentLink: async (transactionId, reason = 'Customer changed payment method') => {
    const res = await api.post(`/payments/${transactionId}/cancel`, { reason });
    return res;
  },
  syncPaymentStatus: async (transactionId) => {
    const res = await api.get(`/payments/${transactionId}`);
    return res;
  },
};
