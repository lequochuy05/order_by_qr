import api from '@shared/api/httpClient.js';

const sessionHeaderConfig = (sessionToken) =>
  sessionToken ? { headers: { 'X-Session-Token': sessionToken } } : undefined;

const recommendationItems = (response) =>
  Array.isArray(response?.items) ? response.items : Array.isArray(response) ? response : [];

export const publicMenuService = {
  getCategories: () => api.get('/public/categories'),
  getAllMenuItems: () => api.get('/public/menu-items'),
  getCombos: () => api.get('/public/combos'),
  getActivePromotions: () => api.get('/public/promotions/active'),
  getSettings: () => api.get('/public/settings'),
  getSessionState: (tableCode) => api.get(`/public/tables/${tableCode}/session-state`),
  startSession: (tableCode, sig) =>
    api.post(`/public/tables/${tableCode}/start-session`, null, {
      params: sig ? { sig } : undefined,
    }),
  heartbeatSession: (sessionToken) =>
    api.post('/public/sessions/heartbeat', { sessionToken }, sessionHeaderConfig(sessionToken)),
  requestPayment: (tableCode, sessionToken) =>
    api.post(
      `/public/tables/${tableCode}/request-payment`,
      { sessionToken },
      sessionHeaderConfig(sessionToken),
    ),
  validateVoucher: (code, subtotal = 0) =>
    api.get('/vouchers/validate', { params: { code, subtotal } }),
  createSelfCheckout: (tableCode, payload, sessionToken) =>
    api.post(
      `/public/tables/${tableCode}/self-checkout`,
      payload,
      sessionHeaderConfig(sessionToken),
    ),
  cancelSelfCheckout: (tableCode, transactionId, payload, sessionToken) =>
    api.post(
      `/public/tables/${tableCode}/self-checkout/${transactionId}/cancel`,
      payload,
      sessionHeaderConfig(sessionToken),
    ),
  syncSelfCheckoutStatus: (tableCode, transactionId, sessionToken) =>
    api.get(`/public/tables/${tableCode}/self-checkout/${transactionId}`, {
      params: { sessionToken },
      ...sessionHeaderConfig(sessionToken),
    }),
  getCurrentOrderByTableCode: (tableCode, sessionToken) =>
    api.get(`/public/tables/code/${tableCode}/current-order`, { params: { sessionToken } }),
  createOrder: (orderData) =>
    api.post('/public/orders', orderData, sessionHeaderConfig(orderData?.sessionToken)),
  getPopularItems: () => api.get('/recommendations/popular').then(recommendationItems),
  getPersonalizedRecommendations: (context) =>
    api.get('/recommendations/personalized', { params: { context } }).then(recommendationItems),
  getCrossSellRecommendations: (itemId) =>
    api.get(`/recommendations/cross-sell/${itemId}`).then(recommendationItems),
  sendAiChat: (message, history = []) => api.post('/public/ai/chat', { message, history }),
};
