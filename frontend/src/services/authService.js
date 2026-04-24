import api from './api';

export const authService = {
  login: async (email, password) => {
    // Gọi đến endpoint /api/users/login từ UserController
    const response = await api.post('/users/login', { email, password });
    return response.data; // Trả về AuthResponse (userId, fullName, role, accessToken)
  },
  
  // Bạn có thể thêm các hàm lấy thông tin user nếu cần
  getProfile: async () => {
    return await api.get('/auth/profile');
  },
};