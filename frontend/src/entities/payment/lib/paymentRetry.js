import { PAYMENT_METHODS, PAYMENT_TRANSACTION_STATUSES } from './paymentStatus.js';

const wait = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));

export const createPaymentWithRetry = async ({
  post,
  orderId,
  paymentMethod,
  voucherCode = null,
  idempotencyKey,
  waitForRetry = wait,
}) => {
  const maxAttempts = paymentMethod === PAYMENT_METHODS.PAYOS ? 5 : 1;

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    const response = await post('/payments', {
      orderId,
      paymentMethod,
      voucherCode,
      idempotencyKey,
    });

    if (response?.status !== PAYMENT_TRANSACTION_STATUSES.CREATING || attempt === maxAttempts) {
      return response;
    }
    await waitForRetry(400);
  }

  return null;
};
