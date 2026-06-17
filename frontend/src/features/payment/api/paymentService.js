import api from '@shared/api/httpClient.js';

export const paymentService = {
  createPaymentLink: async (orderId, voucherCode = null) => {
    const idempotencyKey = crypto.randomUUID
      ? crypto.randomUUID()
      : Math.random().toString(36).substring(2) + Date.now().toString(36);
    const res = await api.post('/payments', {
      orderId,
      paymentMethod: 'PAYOS',
      voucherCode,
      idempotencyKey,
    });
    return res;
  },

  cancelPaymentLink: async (transactionId, reason = 'Khách đổi hình thức thanh toán') => {
    const res = await api.post(`/payments/${transactionId}/cancel`, { reason });
    return res;
  },

  syncPaymentStatus: async (transactionId) => {
    const res = await api.get(`/payments/${transactionId}`);
    return res;
  },
};
