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
        // Ưu tiên detail -> message -> string body -> JSON
        msg = data.detail || data.message || (typeof data === 'string' ? data : JSON.stringify(data));
    } else if (err && err.message) {
        msg = err.message;
    }

    // (Tùy chọn) Việt hóa các lỗi phổ biến tại đây nếu muốn dùng chung cho cả app
    if (msg.includes("already exists")) msg = "Dữ liệu này đã tồn tại trong hệ thống!";

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