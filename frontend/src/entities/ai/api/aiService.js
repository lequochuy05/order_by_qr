import api from '@shared/api/httpClient.js';

export const aiCustomerService = {
  chat: (message, history = []) => api.post('/public/ai/chat', { message, history }),
};

export const aiStaffService = {
  query: (message, history = []) => api.post('/ai/staff/query', { message, history }),
  analyzeNotes: ({ language = 'vi' } = {}) =>
    api.post('/ai/staff/analyze-notes', { language }),
};

export const aiInventoryService = {
  suggestRestock: ({ forecastDays = 7, language = 'vi' } = {}) =>
    api.post('/ai/inventory/restock-suggestions', { forecastDays, language }),
  assistRecipe: ({ menuItemId, currentItems = [], language = 'vi' }) =>
    api.post('/ai/inventory/recipe-assistant', { menuItemId, currentItems, language }),
  scanInvoice: (file, { language = 'vi' } = {}) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('language', language);
    return api.post('/ai/inventory/invoice-ocr', formData, { timeout: 60000 });
  },
};

export const aiMerchandisingService = {
  suggestCombos: ({ language = 'vi' } = {}) =>
    api.post('/ai/merchandising/combo-suggestions', { language }),
  suggestPromotions: ({ language = 'vi' } = {}) =>
    api.post('/ai/merchandising/promotion-suggestions', { language }),
};
