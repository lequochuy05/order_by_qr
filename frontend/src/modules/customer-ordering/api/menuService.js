import api from '@shared/api/httpClient.js';

export const menuService = {
  getCategories: () => api.get('/public/categories'),
  getAllMenuItems: () => api.get('/public/menu-items'),
  getMenuByCategory: (categoryId) => api.get('/public/menu-items', { params: { categoryId } }),
  getCombos: () => api.get('/public/combos'),
  getSettings: () => api.get('/public/settings'),
  getTableByCode: (tableCode) => api.get(`/public/tables/by-code/${tableCode}`),
  getSessionState: (tableCode) => api.get(`/public/tables/${tableCode}/session-state`),
  startSession: (tableCode) => api.post(`/public/tables/${tableCode}/start-session`),
  heartbeatSession: (sessionToken) => api.post('/public/sessions/heartbeat', { sessionToken }),
  getCurrentOrderByTableCode: (tableCode, sessionToken) =>
    api.get(`/public/tables/code/${tableCode}/current-order`, { params: { sessionToken } }),
  createOrder: (orderData) => api.post('/public/orders', orderData),
  getRecommendations: (itemId) => api.get(`/public/recommendations/items/${itemId}`),
  getPopularItems: () => api.get('/public/recommendations/popular'),
  getPersonalizedRecommendations: (context) =>
    api.get('/public/recommendations/personalized', { params: { context } }),
  getCrossSellRecommendations: (itemId) =>
    api.get(`/public/recommendations/cross-sell/${itemId}`),
  sendAiChat: (message, history = []) =>
    api.post('/ai/chat', { message, history }),
};
