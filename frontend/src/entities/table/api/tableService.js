import api from '@shared/api/httpClient.js';

export const tableService = {
  getAll: async () => {
    const res = await api.get('/tables');
    return res;
  },
  getById: async (id) => {
    const res = await api.get(`/tables/${id}`);
    return res;
  },
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
  cancelMerge: async (sessionId) => {
    const res = await api.post(`/table-sessions/${sessionId}/cancel-merge`);
    return res;
  },
  delete: async (id) => {
    await api.delete(`/tables/${id}`);
  },
};
