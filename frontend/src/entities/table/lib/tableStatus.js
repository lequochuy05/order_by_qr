import { getStatusClasses } from '@shared/lib/statusClasses.js';

export const TABLE_STATUS = {
  AVAILABLE: { label: 'Bàn trống', color: 'slate' },
  OCCUPIED: { label: 'Đang có khách', color: 'orange' },
  WAITING_FOR_PAYMENT: { label: 'Chờ tính tiền', color: 'cyan' },
};

export const getTableStatusMeta = (status) => {
  const meta = TABLE_STATUS[status] || { label: status || 'Không rõ', color: 'slate' };
  return { ...meta, classes: getStatusClasses(meta.color) };
};
