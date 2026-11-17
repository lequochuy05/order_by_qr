import { getStatusClasses } from '@shared/lib/statusClasses.js';

export const TABLE_STATUSES = Object.freeze({
  AVAILABLE: 'AVAILABLE',
  OCCUPIED: 'OCCUPIED',
  WAITING_FOR_PAYMENT: 'WAITING_FOR_PAYMENT',
  INACTIVE: 'INACTIVE',
});

export const TABLE_STATUS = {
  [TABLE_STATUSES.AVAILABLE]: { label: 'Bàn trống', color: 'slate' },
  [TABLE_STATUSES.OCCUPIED]: { label: 'Đang có khách', color: 'orange' },
  [TABLE_STATUSES.WAITING_FOR_PAYMENT]: { label: 'Chờ tính tiền', color: 'cyan' },
  [TABLE_STATUSES.INACTIVE]: { label: 'Ngưng hoạt động', color: 'rose' },
};

export const TABLE_STATUS_EN = {
  [TABLE_STATUSES.AVAILABLE]: { label: 'Available', color: 'slate' },
  [TABLE_STATUSES.OCCUPIED]: { label: 'Occupied', color: 'orange' },
  [TABLE_STATUSES.WAITING_FOR_PAYMENT]: { label: 'Awaiting payment', color: 'cyan' },
  [TABLE_STATUSES.INACTIVE]: { label: 'Inactive', color: 'rose' },
};

export const getTableStatusMeta = (status, language = 'vi') => {
  const statusDict = language === 'en' ? TABLE_STATUS_EN : TABLE_STATUS;
  const meta = statusDict[status] || TABLE_STATUS[status] || {
    label: status || (language === 'en' ? 'Unknown' : 'Không rõ'),
    color: 'slate',
  };
  return { ...meta, classes: getStatusClasses(meta.color) };
};
