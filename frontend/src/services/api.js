import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  // Nếu url chưa có chữ /api ở đầu, tự động gắn thêm vào
  if (config.url && !config.url.startsWith('/api') && !config.url.startsWith('http')) {
    config.url = '/api' + (config.url.startsWith('/') ? '' : '/') + config.url;
  }

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

    if (apiResponse && Object.prototype.hasOwnProperty.call(apiResponse, 'success')) {
      if (apiResponse.success) {
        return apiResponse.data;
      } else {
        return Promise.reject(new Error(apiResponse.message || 'Đã xảy ra lỗi nghiệp vụ'));
      }
    }

    return response;
  },
  (error) => {
    if (error.response && error.response.data && error.response.data.message) {
      return Promise.reject(new Error(error.response.data.message));
    }
    return Promise.reject(error);
  }
);

export default api;