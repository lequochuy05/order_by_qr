export const getOrderSubtotalAmount = (order) =>
  order?.subtotalAmount ?? 0;

export const getOrderDiscountAmount = (order) =>
  order?.discountAmount ?? 0;

export const getOrderFinalAmount = (order) =>
  order?.finalAmount ?? 0;

export const getOrderPaidAmount = (order) =>
  order?.paidAmount ?? getOrderFinalAmount(order);
