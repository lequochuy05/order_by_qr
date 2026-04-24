import api from '../api';

export const categoryService = {

    getAll: async () => {
        const res = await api.get('/categories');
        return res.data;
    },
    
    // Tìm kiếm và phân trang
    search: async (q, page = 0, size = 12) => {
        const params = { q, page, size, sort: 'name,asc' };
        const res = await api.get('/categories/search', { params });
        return res.data;
    },

    // Tạo mới danh mục
    create: async (name) => {
        const res = await api.post('/categories', { name });
        return res.data;
    },

    // Cập nhật danh mục
    update: async (id, name) => {
        const res = await api.put(`/categories/${id}`, { name });
        return res.data;
    },

    // Upload ảnh
    uploadImage: async (id, file) => {
        const formData = new FormData();
        formData.append('file', file);
        const res = await api.post(`/categories/${id}/image`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
        });
        return res.data;
    },

    // Xóa danh mục
    delete: async (id) => {
        await api.delete(`/categories/${id}`);
    }
};
