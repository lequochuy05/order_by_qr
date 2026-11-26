export const sameId = (left, right) =>
  left != null && right != null && String(left) === String(right);

export const getPaymentEventOrderId = (message) =>
  message?.orderId ?? message?.id ?? message?.data?.orderId;

export const getPaymentEventTransactionId = (message) =>
  message?.transactionId ?? message?.data?.transactionId;
