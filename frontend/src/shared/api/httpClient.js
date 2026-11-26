import axios from 'axios';

let accessToken = null;
let refreshPromise = null;
const authFailureListeners = new Set();
const API_PREFIX = '/api/v1';
const configuredApiUrl = (import.meta.env.VITE_API_URL || '').replace(/\/+$/, '');
const API_BASE_URL = configuredApiUrl.endsWith(API_PREFIX)
  ? configuredApiUrl
  : `${configuredApiUrl}${API_PREFIX}`;

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

const authClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const setAccessToken = (token) => {
  accessToken = token || null;
};

export const getAccessToken = () => accessToken;

const decodeJwtExpMs = (token) => {
  if (typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    const json =
      typeof globalThis.atob === 'function'
        ? globalThis.atob(padded)
        : null;
    if (!json) return null;
    const payload = JSON.parse(json);
    return typeof payload?.exp === 'number' ? payload.exp * 1000 : null;
  } catch {
    return null;
  }
};

export const isAccessTokenExpiringSoon = (bufferMs = 60_000, nowMs = Date.now()) => {
  if (!accessToken) return true;
  const expMs = decodeJwtExpMs(accessToken);
  if (expMs == null) return false;
  return expMs - nowMs <= bufferMs;
};

export const registerAuthFailureListener = (listener) => {
  if (typeof listener !== 'function') return () => {};
  authFailureListeners.add(listener);
  return () => authFailureListeners.delete(listener);
};

const notifyAuthFailure = () => {
  authFailureListeners.forEach((listener) => {
    try {
      listener();
    } catch {
      // Ignore errors in auth failure cleanup callbacks
    }
  });
};

api.interceptors.request.use((config) => {
  if (config.skipAuth) {
    delete config.headers.Authorization;
  } else if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  // Tự động bỏ Content-Type nếu dữ liệu gửi lên là FormData để trình duyệt tự tính boundary
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type'];
  }

  return config;
});

const unwrapApiResponse = (response) => {
  const apiResponse = response.data;

  if (apiResponse && Object.prototype.hasOwnProperty.call(apiResponse, 'success')) {
    if (apiResponse.success) {
      return apiResponse.data;
    }
    return Promise.reject(createApiError(apiResponse, response));
  }

  return response;
};

const isBackendErrorPayload = (value) =>
  value &&
  typeof value === 'object' &&
  (Object.prototype.hasOwnProperty.call(value, 'code') ||
    Object.prototype.hasOwnProperty.call(value, 'details'));

const normalizeErrorData = (data, fallbackStatus) => {
  const embeddedError = data?.data;
  const payload = isBackendErrorPayload(embeddedError) ? embeddedError : data;
  const message = payload?.message || data?.message || 'Đã xảy ra lỗi';

  return {
    ...payload,
    success: data?.success,
    status: data?.status || fallbackStatus,
    code: payload?.code || data?.code,
    message,
    details: payload?.details || data?.details || {},
    raw: data,
  };
};

const createApiError = (data, response) => {
  const normalized = normalizeErrorData(data, response?.status);
  const apiError = new Error(normalized.message);
  apiError.data = normalized;
  apiError.response = response ? { ...response, data: normalized } : undefined;
  apiError.status = normalized.status || response?.status;
  apiError.code = normalized.code;
  apiError.details = normalized.details;
  return apiError;
};

export const refreshAccessToken = async () => {
  if (!refreshPromise) {
    refreshPromise = authClient
      .post('/auth/refresh')
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

const isAuthEndpoint = (url = '') =>
  url.includes('/auth/login') || url.includes('/auth/refresh') || url.includes('/auth/logout');

// Response interceptor: Tự động bóc tách ApiResponse<T>
api.interceptors.response.use(unwrapApiResponse, async (error) => {
  const originalRequest = error.config;
  const isAdminPath = window.location.pathname.startsWith('/admin');

  if (
    error.response?.status === 401 &&
    originalRequest &&
    !originalRequest._retry &&
    isAdminPath &&
    !isAuthEndpoint(originalRequest.url)
  ) {
    originalRequest._retry = true;
    try {
      const data = await refreshAccessToken();
      if (data?.accessToken) {
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
      }
      return api(originalRequest);
    } catch {
      setAccessToken(null);
      notifyAuthFailure();
    }
  }

  if (error.response && error.response.data) {
    return Promise.reject(createApiError(error.response.data, error.response));
  }
  return Promise.reject(error);
});

export default api;
