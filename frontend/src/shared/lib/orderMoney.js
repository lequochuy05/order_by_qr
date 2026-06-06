export const getOrderSubtotalAmount = (order) =>
  order?.subtotalAmount ?? order?.originalTotal ?? 0;

export const getOrderDiscountAmount = (order) =>
  order?.discountAmount ?? order?.discountVoucher ?? 0;

export const getOrderFinalAmount = (order) =>
  order?.finalAmount ?? order?.totalAmount ?? order?.amount ?? 0;

export const getOrderPaidAmount = (order) =>
  order?.paidAmount ?? getOrderFinalAmount(order);
