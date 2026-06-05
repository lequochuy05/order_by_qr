/**
 * Order Status — Single Source of Truth
 * Import from here in all components that display order/item/table statuses.
 */

// ============================================
// ORDER STATUS
// ============================================

export const ORDER_STATUS = {
  PENDING:          { label: 'Đã nhận đơn',   helper: 'Quán đang kiểm tra',    color: 'amber' },
  SERVING:          { label: 'Đang chuẩn bị',  helper: 'Bếp đang làm món',      color: 'blue' },
  AWAITING_PAYMENT: { label: 'Chờ thanh toán',  helper: 'Tất cả món đã lên đủ',  color: 'cyan' },
  COMPLETED:        { label: 'Đã thanh toán',   helper: 'Cảm ơn đã dùng bữa',   color: 'emerald' },
  CANCELLED:        { label: 'Đã hủy',          helper: 'Đơn đã bị hủy',         color: 'rose' },
};

// ============================================
// ORDER ITEM STATUS (Kitchen workflow)
// ============================================

export const ITEM_STATUS = {
  PENDING:   { label: 'Chờ nấu',       color: 'amber' },
  COOKING:   { label: 'Đang chế biến', color: 'blue' },
  FINISHED:  { label: 'Hoàn thành',    color: 'emerald' },
  CANCELLED: { label: 'Đã hủy',        color: 'rose' },
};

// ============================================
// TABLE STATUS
// ============================================

export const TABLE_STATUS = {
  AVAILABLE:           { label: 'Bàn trống',     color: 'slate' },
  OCCUPIED:            { label: 'Đang có khách',  color: 'orange' },
  WAITING_FOR_PAYMENT: { label: 'Chờ tính tiền', color: 'cyan' },
};

// ============================================
// Tailwind CSS class maps (used by UI components)
// ============================================

const COLOR_CLASSES = {
  amber:   { bg: 'bg-amber-50 dark:bg-amber-500/10',     text: 'text-amber-700 dark:text-amber-300',     border: 'border-amber-200 dark:border-amber-500/30' },
  blue:    { bg: 'bg-blue-50 dark:bg-blue-500/10',       text: 'text-blue-700 dark:text-blue-300',       border: 'border-blue-200 dark:border-blue-500/30' },
  cyan:    { bg: 'bg-cyan-50 dark:bg-cyan-500/10',       text: 'text-cyan-700 dark:text-cyan-300',       border: 'border-cyan-200 dark:border-cyan-500/30' },
  emerald: { bg: 'bg-emerald-50 dark:bg-emerald-500/10', text: 'text-emerald-700 dark:text-emerald-300', border: 'border-emerald-200 dark:border-emerald-500/30' },
  rose:    { bg: 'bg-rose-50 dark:bg-rose-500/10',       text: 'text-rose-700 dark:text-rose-300',       border: 'border-rose-200 dark:border-rose-500/30' },
  indigo:  { bg: 'bg-indigo-50 dark:bg-indigo-500/10',   text: 'text-indigo-700 dark:text-indigo-300',   border: 'border-indigo-200 dark:border-indigo-500/30' },
  slate:   { bg: 'bg-slate-50 dark:bg-slate-800',        text: 'text-slate-600 dark:text-slate-300',     border: 'border-slate-200 dark:border-slate-700' },
  orange:  { bg: 'bg-orange-50 dark:bg-orange-500/10',   text: 'text-orange-600 dark:text-orange-300',   border: 'border-orange-400 dark:border-orange-500/30' },
};

const FALLBACK_COLOR = COLOR_CLASSES.slate;

const toClasses = (colorName) => {
  const c = COLOR_CLASSES[colorName] || FALLBACK_COLOR;
  return `${c.bg} ${c.text} ${c.border}`;
};

// ============================================
// Utility functions
// ============================================

/**
 * Returns label, helper, and Tailwind CSS classes for an order status.
 */
export const getOrderStatusMeta = (status) => {
  const meta = ORDER_STATUS[status] || { label: status || 'Đang cập nhật', helper: '', color: 'slate' };
  return { ...meta, classes: toClasses(meta.color) };
};

/**
 * Returns label and Tailwind CSS classes for an item status.
 */
export const getItemStatusMeta = (status) => {
  const meta = ITEM_STATUS[status] || { label: status || 'Đang cập nhật', color: 'slate' };
  return { ...meta, classes: toClasses(meta.color) };
};

/**
 * Returns label and Tailwind CSS classes for a table status.
 */
export const getTableStatusMeta = (status) => {
  const meta = TABLE_STATUS[status] || { label: status || 'Không rõ', color: 'slate' };
  return { ...meta, classes: toClasses(meta.color) };
};
