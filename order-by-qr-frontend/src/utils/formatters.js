/**
 * Định dạng số thành tiền Việt Nam (VND)
 * Ví dụ: 100000 -> "100.000 ₫"
 */
export const fmtVND = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount || 0);
};

/**
 * Định dạng ngày tháng (dd/MM/yyyy)
 */
export const fmtDate = (dateString) => {
    if (!dateString) return '';
    return new Date(dateString).toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
};

/**
 * Định dạng ngày giờ (dd/MM/yyyy HH:mm)
 */
export const fmtDateTime = (dateString) => {
    if (!dateString) return '';
    return new Date(dateString).toLocaleString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
};