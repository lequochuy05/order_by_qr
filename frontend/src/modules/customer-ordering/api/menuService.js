import api from '@shared/api/httpClient.js';

export const menuService = {
  getCategories: () => api.get('/public/categories'),
  getAllMenuItems: () => api.get('/public/menu-items'),
  getMenuByCategory: (categoryId) => api.get('/public/menu-items', { params: { categoryId } }),
  getCombos: () => api.get('/public/combos'),
  getSettings: () => api.get('/public/settings'),
  getTableByCode: (tableCode) => api.get(`/public/tables/by-code/${tableCode}`),
  getCurrentOrderByTableCode: (tableCode) => api.get(`/public/tables/code/${tableCode}/current-order`),
  createOrder: (orderData) => api.post('/public/orders', orderData),
  getRecommendations: (itemId) => api.get(`/public/recommendations/items/${itemId}`),
  getPopularItems: () => api.get('/public/recommendations/popular'),
  getPersonalizedRecommendations: (time) =>
    api.get('/public/recommendations/personalized', { params: { timeContext: time } }),
  getCrossSellRecommendations: (itemId) =>
    api.get(`/public/recommendations/cross-sell/${itemId}`),
  sendAiChat: (message, history = []) =>
    api.post('/ai/chat', { message, history }),
};
