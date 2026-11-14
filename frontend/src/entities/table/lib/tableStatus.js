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

export const getTableStatusMeta = (status) => {
  const meta = TABLE_STATUS[status] || { label: status || 'Không rõ', color: 'slate' };
  return { ...meta, classes: getStatusClasses(meta.color) };
};
