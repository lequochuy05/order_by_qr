import api from '@shared/api/httpClient.js';

export const menuItemService = {
  // Lấy tất cả hoặc theo danh mục
  getAll: async (categoryId) => {
    // Nếu categoryId tồn tại và khác 'ALL' -> Gọi API lọc
    if (categoryId && categoryId !== 'ALL') {
        const res = await api.get('/menu-items', { params: { categoryId } });
        return res;
    }
    // Ngược lại -> Gọi API lấy tất cả
    const res = await api.get('/menu-items');
    return res;
  },

  // Tạo mới món ăn
  create: async (itemData) => {
    const res = await api.post('/menu-items', itemData);
    return res;
  },

  // Cập nhật món ăn
  update: async (id, itemData) => {
    const res = await api.put(`/menu-items/${id}`, itemData);
    return res;
  },

  // Upload ảnh món ăn
  uploadImage: async (id, file) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post(`/menu-items/${id}/image`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return res;
  },

  // Xóa món ăn
  delete: async (id) => {
    await api.delete(`/menu-items/${id}`);
  }
};
