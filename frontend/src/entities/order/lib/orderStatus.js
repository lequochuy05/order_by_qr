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

const ORDER_STATUS_EN = {
  [ORDER_STATUSES.PENDING]: { label: 'Received', helper: 'Checking order', color: 'amber' },
  [ORDER_STATUSES.SERVING]: { label: 'Preparing', helper: 'Kitchen cooking', color: 'blue' },
  [ORDER_STATUSES.AWAITING_PAYMENT]: { label: 'Awaiting payment', helper: 'All items served', color: 'cyan' },
  [ORDER_STATUSES.COMPLETED]: { label: 'Paid & finished', helper: 'Thank you for dining', color: 'emerald' },
  [ORDER_STATUSES.CANCELLED]: { label: 'Cancelled', helper: 'Order cancelled', color: 'rose' },
};

// ORDER ITEM STATUS (Kitchen workflow)
const ITEM_STATUS = {
  [ORDER_ITEM_STATUSES.PENDING]: { label: 'Chờ nấu', color: 'amber' },
  [ORDER_ITEM_STATUSES.COOKING]: { label: 'Đang chế biến', color: 'blue' },
  [ORDER_ITEM_STATUSES.FINISHED]: { label: 'Hoàn thành', color: 'emerald' },
  [ORDER_ITEM_STATUSES.CANCELLED]: { label: 'Đã hủy', color: 'rose' },
};

const ITEM_STATUS_EN = {
  [ORDER_ITEM_STATUSES.PENDING]: { label: 'Pending', color: 'amber' },
  [ORDER_ITEM_STATUSES.COOKING]: { label: 'Cooking', color: 'blue' },
  [ORDER_ITEM_STATUSES.FINISHED]: { label: 'Finished', color: 'emerald' },
  [ORDER_ITEM_STATUSES.CANCELLED]: { label: 'Cancelled', color: 'rose' },
};

// Utility functions
/**
 * Returns label, helper, and Tailwind CSS classes for an order status.
 */
export const getOrderStatusMeta = (status, language = 'vi') => {
  const statusDict = language === 'en' ? ORDER_STATUS_EN : ORDER_STATUS;
  const meta = statusDict[status] || ORDER_STATUS[status] || {
    label: status || (language === 'en' ? 'Updating' : 'Đang cập nhật'),
    helper: '',
    color: 'slate',
  };
  return { ...meta, classes: getStatusClasses(meta.color) };
};

/**
 * Returns label and Tailwind CSS classes for an item status.
 */
export const getItemStatusMeta = (status, language = 'vi') => {
  const statusDict = language === 'en' ? ITEM_STATUS_EN : ITEM_STATUS;
  const meta = statusDict[status] || ITEM_STATUS[status] || {
    label: status || (language === 'en' ? 'Updating' : 'Đang cập nhật'),
    color: 'slate',
  };
  return { ...meta, classes: getStatusClasses(meta.color) };
};
