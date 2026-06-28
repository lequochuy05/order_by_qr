import { BUSINESS_TIME_ZONE } from './businessTime.js';

/**
 * Định dạng số thành tiền Việt Nam (VND)
 */
export const fmtVND = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    currencyDisplay: 'narrowSymbol',
  }).format(amount || 0);
};

/**
 * Định dạng ngày (dd/MM/yyyy)
 */
export const fmtDate = (dateString) => {
  if (!dateString) return '';
  return new Date(dateString).toLocaleDateString('vi-VN', {
    timeZone: BUSINESS_TIME_ZONE,
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};

/**
 * Định dạng ngày giờ (dd/MM/yyyy HH:mm)
 */
export const fmtDateTime = (dateString) => {
  if (!dateString) return '';
  return new Date(dateString).toLocaleString('vi-VN', {
    timeZone: BUSINESS_TIME_ZONE,
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

/**
 * Định dạng giờ (HH:mm)
 */
export const fmtTime = (dateString) => {
  if (!dateString) return '';
  return new Date(dateString).toLocaleTimeString('vi-VN', {
    timeZone: BUSINESS_TIME_ZONE,
    hour: '2-digit',
    minute: '2-digit',
  });
};

/**
 * Định dạng số tiền VND dạng compact cho chart axis (e.g. 1.5tr, 200k)
 */
export const fmtCompactVND = (value) => {
  if (value >= 1_000_000) return `${Math.round(value / 1_000_000)}tr`;
  if (value >= 1_000) return `${Math.round(value / 1_000)}k`;
  return value;
};

/**
 * Định dạng vai trò người dùng
 */
export const fmtRole = (role) => {
  const roles = {
    MANAGER: 'Quản lý',
    STAFF: 'Nhân viên',
    CHEF: 'Bếp',
  };
  return roles[role] || role;
};

export const USER_STATUS = {
  ACTIVE: { label: 'Hoạt động', color: 'bg-green-100 text-green-700' },
  INACTIVE: { label: 'Ngừng hoạt động', color: 'bg-gray-100 text-gray-600' },
  BANNED: { label: 'Đã khóa', color: 'bg-red-100 text-red-600' },
};

/**
 * Định dạng trạng thái
 * Hỗ trợ: Combo/Món ăn (active), Nhân viên (staff)
 */
export const fmtStatus = (type, status) => {
  const maps = {
    active: {
      true: { label: 'Đang kinh doanh', color: 'bg-green-100 text-green-700' },
      false: { label: 'Tạm ngưng', color: 'bg-gray-100 text-gray-500' },
    },
    staff: {
      ...USER_STATUS,
      true: USER_STATUS.ACTIVE,
      false: USER_STATUS.BANNED,
    },
  };

  return maps[type]?.[status] || { label: status, color: 'bg-gray-100 text-gray-800' };
};
