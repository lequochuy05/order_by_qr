import api from '../api';

export const paymentService = {
    createPaymentLink: async (orderId) => {
        const res = await api.post('/payments/payos/create', { orderId });
        return res;
    },

    cancelPaymentLink: async (transactionId, reason = 'Khách đổi hình thức thanh toán') => {
        const res = await api.post(`/payments/payos/${transactionId}/cancel`, { reason });
        return res;
    },

    syncPaymentStatus: async (transactionId) => {
        const res = await api.get(`/payments/payos/${transactionId}/status`);
        return res;
    }
};
