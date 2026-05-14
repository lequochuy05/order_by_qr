import axios from 'axios';

let accessToken = null;
let refreshPromise = null;

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

const authClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const setAccessToken = (token) => {
  accessToken = token || null;
};

export const getAccessToken = () => accessToken;

const normalizeApiUrl = (config) => {
  if (config.url && !config.url.startsWith('/api') && !config.url.startsWith('http')) {
    config.url = '/api' + (config.url.startsWith('/') ? '' : '/') + config.url;
  }
};

api.interceptors.request.use((config) => {
  normalizeApiUrl(config);

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

authClient.interceptors.request.use((config) => {
  normalizeApiUrl(config);
  return config;
});

const unwrapApiResponse = (response) => {
  const apiResponse = response.data;

  if (apiResponse && Object.prototype.hasOwnProperty.call(apiResponse, 'success')) {
    if (apiResponse.success) {
      return apiResponse.data;
    }
    return Promise.reject(new Error(apiResponse.message || 'Đã xảy ra lỗi nghiệp vụ'));
  }

  return response;
};

export const refreshAccessToken = async () => {
  if (!refreshPromise) {
    refreshPromise = authClient.post('/users/refresh')
      .then(unwrapApiResponse)
      .then((data) => {
        setAccessToken(data?.accessToken);
        return data;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
};

const isAuthEndpoint = (url = '') => (
  url.includes('/users/login') ||
  url.includes('/users/refresh') ||
  url.includes('/users/logout')
);

// Response interceptor: Tự động bóc tách ApiResponse<T>
api.interceptors.response.use(
  unwrapApiResponse,
  async (error) => {
    const originalRequest = error.config;
    const isAdminPath = window.location.pathname.startsWith('/admin');

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry
      && isAdminPath && !isAuthEndpoint(originalRequest.url)) {
      originalRequest._retry = true;
      try {
        const data = await refreshAccessToken();
        if (data?.accessToken) {
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        }
        return api(originalRequest);
      } catch {
        setAccessToken(null);
      }
    }

    if (error.response && error.response.data && error.response.data.message) {
      return Promise.reject(new Error(error.response.data.message));
    }
    return Promise.reject(error);
  }
);

export default api;
