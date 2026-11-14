import { PAYMENT_METHODS, PAYMENT_TRANSACTION_STATUSES } from './paymentStatus.js';

const wait = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));

export const createPaymentWithRetry = async ({
  post,
  orderId,
  paymentMethod,
  voucherCode = null,
  idempotencyKey,
  cashReceived = null,
  changeAmount = null,
  confirmUnfinishedItems = false,
  waitForRetry = wait,
}) => {
  const maxAttempts = paymentMethod === PAYMENT_METHODS.PAYOS ? 5 : 1;

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    const payload = {
      orderId,
      paymentMethod,
      voucherCode,
      idempotencyKey,
    };
    if (cashReceived != null) payload.cashReceived = cashReceived;
    if (changeAmount != null) payload.changeAmount = changeAmount;
    if (confirmUnfinishedItems) payload.confirmUnfinishedItems = true;

    const response = await post('/payments', payload);

    if (response?.status !== PAYMENT_TRANSACTION_STATUSES.CREATING || attempt === maxAttempts) {
      return response;
    }
    await waitForRetry(400);
  }

  return null;
};
