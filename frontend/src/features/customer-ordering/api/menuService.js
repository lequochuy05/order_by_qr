import api from '@shared/api/httpClient.js';

const sessionHeaderConfig = (sessionToken) =>
  sessionToken ? { headers: { 'X-Session-Token': sessionToken } } : undefined;

const recommendationItems = (response) =>
  Array.isArray(response?.items) ? response.items : Array.isArray(response) ? response : [];

export const menuService = {
  getCategories: () => api.get('/public/categories'),
  getAllMenuItems: () => api.get('/public/menu-items'),
  getCombos: () => api.get('/public/combos'),
  getSettings: () => api.get('/public/settings'),
  getSessionState: (tableCode) => api.get(`/public/tables/${tableCode}/session-state`),
  startSession: (tableCode) => api.post(`/public/tables/${tableCode}/start-session`),
  heartbeatSession: (sessionToken) =>
    api.post('/public/sessions/heartbeat', { sessionToken }, sessionHeaderConfig(sessionToken)),
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
