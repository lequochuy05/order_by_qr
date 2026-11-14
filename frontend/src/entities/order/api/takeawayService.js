import { ORDER_TYPES } from '../lib/orderStatus.js';
import api from '@shared/api/httpClient.js';

export const takeawayService = {
  getActiveOrders: async () => {
    const res = await api.get('/orders/active-takeaway');
    return res;
  },

  createOrder: async (data) => {
    const res = await api.post('/orders', { ...data, orderType: ORDER_TYPES.TAKEAWAY });
    return res;
  },

  previewOrder: async (data) => {
    const res = await api.post('/orders/preview', { ...data, orderType: ORDER_TYPES.TAKEAWAY });
    return res;
  },
};
