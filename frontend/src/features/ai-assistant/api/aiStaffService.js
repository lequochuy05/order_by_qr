import api from '@shared/api/httpClient.js';

export const aiStaffService = {
  query: (message, history = []) => api.post('/ai/staff/query', { message, history }),
};
