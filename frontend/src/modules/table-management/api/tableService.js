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
        const res = await api.post('/tables',data);
        return res;
    },

    update: async(id, data) => {
        const res = await api.put(`/tables/${id}`, data);
        return res
    },
    delete: async(id) => {
        await api.delete(`/tables/${id}`);
    },

}