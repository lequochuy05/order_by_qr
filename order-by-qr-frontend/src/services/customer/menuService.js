import api from '../api';

export const menuService = {
  getCategories: () => api.get('/categories'),
  getAllMenuItems: () => api.get('/menu'),
  getMenuByCategory: (categoryId) => api.get(`/menu/category/${categoryId}`),
  getCombos: () => api.get('/combos'),
  getTableByCode: (tableCode) => api.get(`/tables/code/${tableCode}`),
  createOrder: (orderData) => api.post('/orders', orderData),
  getRecommendations: (itemId) => api.get(`/recommendations/item/${itemId}`),
  getPopularItems: () => api.get('/recommendations/popular'),
  getPersonalizedRecommendations: (time, weather) =>
    api.get(`/recommendations/personalized?timeContext=${time}&weatherContext=${weather}`),
  getCrossSellRecommendations: (itemId) =>
    api.get(`/recommendations/cross-sell/${itemId}`),
};