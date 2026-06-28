import api from '@shared/api/httpClient.js';
import { createPaymentWithRetry } from '../lib/paymentRetry.js';

const createIdempotencyKey = () =>
  crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2) + Date.now().toString(36);

export const paymentService = {
  createPayment: async (orderId, paymentMethod, voucherCode = null) => {
    const idempotencyKey = createIdempotencyKey();
    return createPaymentWithRetry({
      post: (url, payload) => api.post(url, payload),
      orderId,
      paymentMethod,
      voucherCode,
      idempotencyKey,
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
