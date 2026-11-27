import api from '@shared/api/httpClient.js';

export const takeawayService = {
  getActiveOrders: async () => {
    const res = await api.get('/orders/active-takeaway');
    return res;
  },
};
