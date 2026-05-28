import { useState, useCallback } from 'react';

const tryParseJson = (value) => {
  if (typeof value !== 'string') return value;

  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
};

const getErrorPayload = (err) => {
  if (!err) return {};
  if (err.response?.data) return tryParseJson(err.response.data);
  if (err.data) return tryParseJson(err.data);
  if (typeof err === 'string') return tryParseJson(err);
  return err;
};

const buildErrorMessage = (err) => {
  const data = getErrorPayload(err);
  let msg = 'Có lỗi xảy ra, vui lòng thử lại.';
  const details = [];

  if (data && typeof data === 'object') {
    msg = data.detail || data.message || data.error || err?.message || msg;

    if (data.data && typeof data.data === 'object') {
      Object.entries(data.data).forEach(([field, message]) => {
        details.push(`${field}: ${message}`);
      });
    }

    const status = data.status || err?.status || err?.response?.status;
    const title = data.title || err?.response?.statusText;
    const instance = data.instance || err?.response?.config?.url;

    if (status) details.push(`Mã lỗi: ${status}${title ? ` - ${title}` : ''}`);
    if (instance) details.push(`Endpoint: ${instance}`);
  } else if (typeof data === 'string' && data.trim()) {
    msg = data;
  } else if (err?.message) {
    msg = err.message;
  }

  if (msg.includes('already exists')) msg = 'Dữ liệu này đã tồn tại trong hệ thống!';
  if (msg === 'Network Error') msg = 'Không thể kết nối đến máy chủ!';

  return details.length > 0 ? `${msg}\n\n${details.join('\n')}` : msg;
};

export const useStatusModal = () => {
  const [statusModal, setStatusModal] = useState({
    isOpen: false,
    type: 'success',
    title: '',
    message: ''
  });

  // Hàm hiển thị thành công
  const showSuccess = useCallback((msg, title = 'Thành công!') => {
    setStatusModal({
      isOpen: true,
      type: 'success',
      title: title,
      message: msg
    });
  }, []);

  // Hàm hiển thị lỗi "thông minh"
  const showError = useCallback((err, title = 'Thao tác thất bại') => {
    setStatusModal({
      isOpen: true,
      type: 'error',
      title: title,
      message: buildErrorMessage(err)
    });
  }, []);

  // Hàm đóng modal
  const closeStatusModal = useCallback(() => {
    setStatusModal(prev => ({ ...prev, isOpen: false }));
  }, []);

  return {
    statusModal,      // State để truyền vào Component
    showSuccess,      // Hàm gọi khi thành công
    showError,        // Hàm gọi khi lỗi
    closeStatusModal  // Hàm đóng
  };
};
