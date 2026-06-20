import api from '@shared/api/httpClient.js';

const createIdempotencyKey = () =>
  crypto.randomUUID
    ? crypto.randomUUID()
    : Math.random().toString(36).substring(2) + Date.now().toString(36);

const wait = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));

export const paymentService = {
  createPayment: async (orderId, paymentMethod, voucherCode = null) => {
    const maxAttempts = paymentMethod === 'PAYOS' ? 5 : 1;

    for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
      const idempotencyKey = paymentMethod === 'PAYOS' ? createIdempotencyKey() : undefined;
      const response = await api.post('/payments', {
        orderId,
        paymentMethod,
        voucherCode,
        idempotencyKey,
      });

      if (response?.status !== 'CREATING' || attempt === maxAttempts) {
        return response;
      }
      await wait(400);
    }

    return null;
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
