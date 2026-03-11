import { useState, useCallback } from 'react';

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
    let msg = "Có lỗi xảy ra, vui lòng thử lại.";

    // Logic bóc tách lỗi từ Backend (Spring Boot)
    if (err && err.response && err.response.data) {
      const data = err.response.data;

      // Ưu tiên lấy thuộc tính 'error' (do GlobalExceptionHandler trả về)
      if (data.error) {
        msg = data.error;
      } else if (data.message) {
        msg = data.message;
      } else if (data.detail) {
        msg = data.detail;
      } else if (typeof data === 'string') {
        msg = data;
      } else if (typeof data === 'object') {
        // Xử lý lỗi từ @Valid (Backend trả về Map { "tên_trường": "Lỗi gì đó" })
        // Sẽ lấy thông báo lỗi đầu tiên trong danh sách để hiển thị
        const errors = Object.values(data);
        if (errors.length > 0) {
          msg = errors[0];
        }
      }
    } else if (err && err.message) {
      msg = err.message;
    }

    // (Tùy chọn) Việt hóa các lỗi phổ biến
    if (msg.includes("already exists")) msg = "Dữ liệu này đã tồn tại trong hệ thống!";
    if (msg === "Network Error") msg = "Không thể kết nối đến máy chủ!";

    setStatusModal({
      isOpen: true,
      type: 'error',
      title: title,
      message: msg
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