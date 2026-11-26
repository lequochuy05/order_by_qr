import api from '@shared/api/httpClient.js';

export const tableService = {
  create: async (data) => {
    const res = await api.post('/tables', data);
    return res;
  },
  update: async (id, data) => {
    const res = await api.put(`/tables/${id}`, data);
    return res;
  },
  updateStatus: async (id, status, version) => {
    const res = await api.patch(`/tables/${id}/status`, { status, version });
    return res;
  },
  regenerateQr: async (id) => {
    const res = await api.post(`/tables/${id}/regenerate-qr`);
    return res;
  },
  merge: async (data) => {
    const res = await api.post('/table-sessions/merge', data);
    return res;
  },
  closeEmptySession: async (sessionId) => {
    const res = await api.post(`/table-sessions/${sessionId}/close-empty`);
    return res;
  },
  delete: async (id) => {
    await api.delete(`/tables/${id}`);
  },
  getTableBoard: () => api.get('/orders/table-board'),
  getSessionState: (tableCode) => api.get(`/public/tables/${tableCode}/session-state`),
  startSession: (tableCode, sig) =>
    api.post(`/public/tables/${tableCode}/start-session`, null, {
      params: sig ? { sig } : undefined,
    }),
  getCurrentOrderByTableCode: (tableCode, sessionToken) =>
    api.get(`/public/tables/code/${tableCode}/current-order`, { params: { sessionToken } }),
};
