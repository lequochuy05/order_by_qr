import api from '@shared/api/httpClient.js';

export const staffService = {
    // Lấy danh sách
    getAll: async () => {
        const res = await api.get('/users');
        return res;
    },

    // Tạo mới (trả về UserDto có id)
    create: async (data) => {
        const res = await api.post('/users', data); 
        return res;
    },

    // Cập nhật thông tin
    update: async (id, data) => {
        const res = await api.put(`/users/${id}`, data);
        return res;
    },

    // Upload Avatar (API riêng biệt)
    uploadAvatar: async (id, file) => {
        const formData = new FormData();
        formData.append('file', file);
        
        const res = await api.post(`/users/${id}/avatar`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return res;
    },

    // Xóa
    delete: async (id) => {
        await api.delete(`/users/${id}`);
    }
};