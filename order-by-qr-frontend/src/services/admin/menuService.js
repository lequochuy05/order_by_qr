import api from '../api';

export const menuItemService = {
  // Lấy tất cả hoặc theo danh mục
  getAll: async (categoryId) => {
    // Nếu categoryId tồn tại và khác 'ALL' -> Gọi API lọc
    if (categoryId && categoryId !== 'ALL') {
        const res = await api.get(`/menu/category/${categoryId}`);
        return res.data;
    }
    // Ngược lại -> Gọi API lấy tất cả
    const res = await api.get('/menu');
    return res.data;
  },

  // Tạo mới món ăn
  create: async (itemData) => {
    const res = await api.post('/menu', itemData);
    return res.data;
  },

  // Cập nhật món ăn
  update: async (id, itemData) => {
    const res = await api.put(`/menu/${id}`, itemData);
    return res.data;
  },

  // Upload ảnh món ăn
  uploadImage: async (id, file) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post(`/menu/${id}/image`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return res.data;
  },

  // Xóa món ăn
  delete: async (id) => {
    await api.delete(`/menu/${id}`);
  }
};