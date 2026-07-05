import { getStatusClasses } from '@shared/lib/statusClasses.js';
import { ORDER_ITEM_STATUSES, ORDER_STATUSES } from './orderConstants.js';

export {
  ORDER_ITEM_STATUSES,
  ORDER_PAYMENT_STATUSES,
  ORDER_STATUSES,
  ORDER_TYPES,
} from './orderConstants.js';

export const ORDER_STATUS = {
  [ORDER_STATUSES.PENDING]: { label: 'Đã nhận đơn', helper: 'Quán đang kiểm tra', color: 'amber' },
  [ORDER_STATUSES.SERVING]: { label: 'Đang chuẩn bị', helper: 'Bếp đang làm món', color: 'blue' },
  [ORDER_STATUSES.AWAITING_PAYMENT]: { label: 'Chờ thanh toán', helper: 'Tất cả món đã lên đủ', color: 'cyan' },
  [ORDER_STATUSES.COMPLETED]: { label: 'Đã thanh toán', helper: 'Cảm ơn đã dùng bữa', color: 'emerald' },
  [ORDER_STATUSES.CANCELLED]: { label: 'Đã hủy', helper: 'Đơn đã bị hủy', color: 'rose' },
};

// ORDER ITEM STATUS (Kitchen workflow)
export const ITEM_STATUS = {
  [ORDER_ITEM_STATUSES.PENDING]: { label: 'Chờ nấu', color: 'amber' },
  [ORDER_ITEM_STATUSES.COOKING]: { label: 'Đang chế biến', color: 'blue' },
  [ORDER_ITEM_STATUSES.FINISHED]: { label: 'Hoàn thành', color: 'emerald' },
  [ORDER_ITEM_STATUSES.CANCELLED]: { label: 'Đã hủy', color: 'rose' },
};

// Utility functions
/**
 * Returns label, helper, and Tailwind CSS classes for an order status.
 */
export const getOrderStatusMeta = (status) => {
  const meta = ORDER_STATUS[status] || {
    label: status || 'Đang cập nhật',
    helper: '',
    color: 'slate',
  };
  return { ...meta, classes: getStatusClasses(meta.color) };
};

/**
 * Returns label and Tailwind CSS classes for an item status.
 */
export const getItemStatusMeta = (status) => {
  const meta = ITEM_STATUS[status] || { label: status || 'Đang cập nhật', color: 'slate' };
  return { ...meta, classes: getStatusClasses(meta.color) };
};
