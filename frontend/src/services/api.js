import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: Tự động bóc tách ApiResponse<T>
api.interceptors.response.use(
  (response) => {
    const apiResponse = response.data;

    // Nếu response có cấu trúc ApiResponse (success, message, data)
    if (apiResponse && Object.prototype.hasOwnProperty.call(apiResponse, 'success')) {
      if (apiResponse.success) {
        return apiResponse.data; // Trả về T trực tiếp cho service
      } else {
        // Ném lỗi từ nghiệp vụ backend
        return Promise.reject(new Error(apiResponse.message || 'Đã xảy ra lỗi nghiệp vụ'));
      }
    }

    return response; // Trả về raw response nếu không khớp format (ví dụ external API)
  },
  (error) => {
    // Xử lý lỗi HTTP (401, 403, 500...)
    if (error.response && error.response.data && error.response.data.message) {
      return Promise.reject(new Error(error.response.data.message));
    }
    return Promise.reject(error);
  }
);

export default api;