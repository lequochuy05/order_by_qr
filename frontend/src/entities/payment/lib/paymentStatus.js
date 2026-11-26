// eslint-disable-next-line no-restricted-imports -- relative path required for Node ESM unit tests
export { PAYMENT_METHODS } from '../../../shared/lib/paymentConstants.js';

export const PAYMENT_TRANSACTION_STATUSES = Object.freeze({
  CREATING: 'CREATING',
  PENDING: 'PENDING',
  PAID: 'PAID',
  CANCELLED: 'CANCELLED',
  EXPIRED: 'EXPIRED',
  FAILED: 'FAILED',
});
