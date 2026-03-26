import api from '../api';

export const tableService = {
    getAll: async () => {
        const res = await api.get('/tables');
        return res.data;
    },

    getById: async (id) => {
        const res = await api.get(`/tables/${id}`);
        return res.data;
    },

    create: async (data) => {
        const res = await api.post('/tables',data);
        return res.data;
    },

    update: async(id, data) => {
        const res = await api.patch(`/tables/${id}`, data);
        return res.data
    },
    delete: async(id) => {
        await api.delete(`/tables/${id}`);
    },

}