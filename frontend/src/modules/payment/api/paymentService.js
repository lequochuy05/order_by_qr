import api from '@shared/api/httpClient.js';

export const paymentService = {
    createPaymentLink: async (orderId, voucherCode = null, createdById = null) => {
        const res = await api.post('/payments/payos', { orderId, voucherCode, createdById });
        return res;
    },

    cancelPaymentLink: async (transactionId, reason = 'Khách đổi hình thức thanh toán') => {
        const res = await api.post(`/payments/payos/${transactionId}/cancellation`, { reason });
        return res;
    },

    syncPaymentStatus: async (transactionId) => {
        const res = await api.get(`/payments/payos/${transactionId}`);
        return res;
    }
};
