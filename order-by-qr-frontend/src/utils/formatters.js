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
 * Định dạng ngày (dd/MM/yyyy)
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

/**
 * Định dạng giờ (HH:mm)
 */
export const fmtTime = (dateString) => {
    if (!dateString) return '';
    return new Date(dateString).toLocaleTimeString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit'
    });
};

/**
 * Định dạng vai trò người dùng
 */
export const fmtRole = (role) => {
    const roles = {
        'MANAGER': 'Quản lý',
        'STAFF': 'Nhân viên',
        'CHEF': 'Bếp'
    };
    return roles[role] || role;
};

/**
 * Định dạng trạng thái
 * Hỗ trợ: Đơn hàng, Bàn, Combo/Món ăn, Bếp, Nhân viên
 */
export const fmtStatus = (type, status) => {
    const maps = {
        order: {
            'PENDING': { label: 'Đang chờ', color: 'bg-yellow-100 text-yellow-700' },
            'CONFIRMED': { label: 'Đã xác nhận', color: 'bg-blue-100 text-blue-700' },
            'PAID': { label: 'Đã thanh toán', color: 'bg-green-100 text-green-700' },
            'CANCELLED': { label: 'Đã hủy', color: 'bg-red-100 text-red-700' },
            'COMPLETED': { label: 'Hoàn tất', color: 'bg-green-100 text-green-700' },
            'FINISHED': { label: 'Hoàn thành', color: 'bg-emerald-100 text-emerald-700' }
        },
        table: {
            'AVAILABLE': { label: 'Bàn trống', color: 'bg-gray-100 text-gray-400' },
            'OCCUPIED': { label: 'Đang có khách', color: 'text-orange-500' },
            'WAITING_FOR_PAYMENT': { label: 'Chờ tính tiền', color: 'text-green-500' },
        },
        active: {
            true: { label: 'Đang kinh doanh', color: 'bg-green-100 text-green-700' },
            false: { label: 'Tạm ngưng', color: 'bg-gray-100 text-gray-500' }
        },
        staff: {
            true: { label: 'Hoạt động', color: 'bg-green-100 text-green-700' },
            false: { label: 'Đã khóa', color: 'bg-red-100 text-red-600' }
        },
        kitchen: {
            'PENDING': { label: 'Chờ chế biến', color: 'bg-yellow-50 text-yellow-600' },
            'COOKING': { label: 'Đang làm', color: 'bg-blue-50 text-blue-600' },
            'READY': { label: 'Đã xong', color: 'bg-green-50 text-green-600' },
            'SERVED': { label: 'Đã phục vụ', color: 'bg-gray-50 text-gray-500' },
            'CANCELLED': { label: 'Đã hủy', color: 'bg-red-50 text-red-500' }
        }
    };

    return maps[type]?.[status] || { label: status, color: 'bg-gray-100 text-gray-800' };
};