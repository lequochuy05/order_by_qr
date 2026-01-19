import api from '../api';

export const comboService = {
  // Lấy toàn bộ combo
  getAll: async () => {
    const res = await api.get('/combos');
    return res.data;
  },

  // Lấy chi tiết combo theo ID
  getById: async (id) => {
    const res = await api.get(`/combos/${id}`);
    return res.data;
  },

  // Tạo mới combo
  create: async (data) => {
    const res = await api.post('/combos', data);
    return res.data;
  },

  // Cập nhật combo
  update: async (id, data) => {
    const res = await api.put(`/combos/${id}`, data);
    return res.data;
  },

  // Xóa combo
  delete: async (id) => {
    await api.delete(`/combos/${id}`);
  },

  // Bật/tắt trạng thái kinh doanh
  toggleActive: async (id) => {
    const res = await api.patch(`/combos/${id}/toggle-active`);
    return res.data;
  }
};