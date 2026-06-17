import { getStatusClasses } from '@shared/lib/statusClasses.js';

export const ORDER_STATUS = {
  PENDING:          { label: 'Đã nhận đơn',   helper: 'Quán đang kiểm tra',    color: 'amber' },
  SERVING:          { label: 'Đang chuẩn bị',  helper: 'Bếp đang làm món',      color: 'blue' },
  AWAITING_PAYMENT: { label: 'Chờ thanh toán',  helper: 'Tất cả món đã lên đủ',  color: 'cyan' },
  COMPLETED:        { label: 'Đã thanh toán',   helper: 'Cảm ơn đã dùng bữa',   color: 'emerald' },
  CANCELLED:        { label: 'Đã hủy',          helper: 'Đơn đã bị hủy',         color: 'rose' },
};

// ORDER ITEM STATUS (Kitchen workflow)
export const ITEM_STATUS = {
  PENDING:   { label: 'Chờ nấu',       color: 'amber' },
  COOKING:   { label: 'Đang chế biến', color: 'blue' },
  FINISHED:  { label: 'Hoàn thành',    color: 'emerald' },
  CANCELLED: { label: 'Đã hủy',        color: 'rose' },
};

// Utility functions
/**
 * Returns label, helper, and Tailwind CSS classes for an order status.
 */
export const getOrderStatusMeta = (status) => {
  const meta = ORDER_STATUS[status] || { label: status || 'Đang cập nhật', helper: '', color: 'slate' };
  return { ...meta, classes: getStatusClasses(meta.color) };
};

/**
 * Returns label and Tailwind CSS classes for an item status.
 */
export const getItemStatusMeta = (status) => {
  const meta = ITEM_STATUS[status] || { label: status || 'Đang cập nhật', color: 'slate' };
  return { ...meta, classes: getStatusClasses(meta.color) };
};
