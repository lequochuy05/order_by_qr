import assert from 'node:assert/strict';
import test from 'node:test';

import {
  calculateSelfCheckoutBreakdown,
  calculateSelfCheckoutSummary,
  canCustomerRequestPayment,
  createSelfCheckoutWithRetry,
  getSelfCheckoutStorageKey,
  getVoucherValidationError,
  hasUnfinishedOrderItems,
  isCustomerSelfCheckoutAvailable,
  selectBestActivePromotion,
} from '../../src/features/customer-menu/lib/selfCheckoutHelpers.js';

test('isCustomerSelfCheckoutAvailable checks both self-checkout toggle and online payment', () => {
  assert.equal(isCustomerSelfCheckoutAvailable({}), false);
  assert.equal(
    isCustomerSelfCheckoutAvailable({
      customerSelfCheckoutEnabled: false,
      onlinePaymentEnabled: true,
    }),
    false,
  );
  assert.equal(
    isCustomerSelfCheckoutAvailable({
      customerSelfCheckoutEnabled: true,
      onlinePaymentEnabled: false,
    }),
    false,
  );
  assert.equal(
    isCustomerSelfCheckoutAvailable({
      customerSelfCheckoutEnabled: true,
      onlinePaymentEnabled: true,
    }),
    true,
  );
});

test('calculateSelfCheckoutSummary computes subtotal, voucher discount, and final amount', () => {
  const order = {
    id: 10,
    subtotalAmount: 150000,
    discountAmount: 0,
    finalAmount: 150000,
    items: [
      { id: 1, status: 'FINISHED', lineTotal: 100000 },
      { id: 2, status: 'FINISHED', lineTotal: 50000 },
      { id: 3, status: 'CANCELLED', lineTotal: 30000 },
    ],
  };

  assert.deepEqual(calculateSelfCheckoutSummary(order, null), {
    subtotalAmount: 150000,
    discountAmount: 0,
    finalAmount: 150000,
  });

  assert.deepEqual(
    calculateSelfCheckoutSummary(order, {
      code: 'SALE20',
      applicable: true,
      appliedDiscountAmount: 30000,
      finalAmount: 120000,
    }),
    {
      subtotalAmount: 150000,
      discountAmount: 30000,
      finalAmount: 120000,
    },
  );
});

test('calculateSelfCheckoutSummary preserves automatic promotion discount and combines with voucher', () => {
  const promoOrder = {
    id: 11,
    subtotalAmount: 200000,
    discountAmount: 30000,
    finalAmount: 170000,
    voucherCode: null,
  };

  assert.deepEqual(calculateSelfCheckoutSummary(promoOrder, null), {
    subtotalAmount: 200000,
    discountAmount: 30000,
    finalAmount: 170000,
  });

  assert.deepEqual(
    calculateSelfCheckoutSummary(promoOrder, {
      code: 'VIP20',
      applicable: true,
      appliedDiscountAmount: 20000,
      finalAmount: 180000,
    }),
    {
      subtotalAmount: 200000,
      discountAmount: 50000,
      finalAmount: 150000,
    },
  );
});

test('selectBestActivePromotion chooses active promotion with highest discount percent', () => {
  const best = selectBestActivePromotion([
    { id: 1, name: 'Inactive 50%', discountPercent: 50, active: false },
    { id: 2, name: 'Happy Hour 10%', discountPercent: 10, active: true },
    { id: 3, name: 'Flash Sale 20%', discountPercent: 20, active: true },
  ]);

  assert.equal(best?.id, 3);
  assert.equal(selectBestActivePromotion([]), null);
});

test('calculateSelfCheckoutBreakdown separates promotion discount and voucher discount cleanly', () => {
  const order = {
    id: 12,
    subtotalAmount: 200000,
    discountAmount: 30000,
    finalAmount: 170000,
    voucherCode: null,
  };
  const promotion = { id: 3, name: 'Giờ vàng trưa', discountPercent: 15, active: true };
  const appliedVoucher = {
    code: 'SALE20K',
    applicable: true,
    appliedDiscountAmount: 20000,
    finalAmount: 180000,
  };

  assert.deepEqual(calculateSelfCheckoutBreakdown(order, appliedVoucher, promotion, null), {
    subtotalAmount: 200000,
    discountAmount: 50000,
    promotionDiscountAmount: 30000,
    voucherDiscountAmount: 20000,
    promotionName: 'Giờ vàng trưa',
    promotionPercent: 15,
    finalAmount: 150000,
  });

  assert.deepEqual(
    calculateSelfCheckoutBreakdown(order, appliedVoucher, promotion, {
      subtotalAmount: 200000,
      discountAmount: 50000,
      finalAmount: 150000,
      voucherCode: 'SALE20K',
    }),
    {
      subtotalAmount: 200000,
      discountAmount: 50000,
      promotionDiscountAmount: 30000,
      voucherDiscountAmount: 20000,
      promotionName: 'Giờ vàng trưa',
      promotionPercent: 15,
      finalAmount: 150000,
    },
  );
});

test('getVoucherValidationError translates known statuses', () => {
  assert.equal(getVoucherValidationError({ applicable: true }), '');
  assert.match(getVoucherValidationError({ status: 'EXPIRED', applicable: false }), /hết hạn/);
  assert.match(getVoucherValidationError({ status: 'EXHAUSTED', applicable: false }), /hết lượt/);
  assert.match(getVoucherValidationError({ status: 'NOT_FOUND', applicable: false }), /không tồn tại/);
});

test('hasUnfinishedOrderItems and canCustomerRequestPayment strictly require all billable items finished', () => {
  assert.equal(
    hasUnfinishedOrderItems({ items: [{ status: 'FINISHED' }, { status: 'CANCELLED' }] }),
    false,
  );
  assert.equal(
    hasUnfinishedOrderItems({ items: [{ status: 'FINISHED' }, { status: 'COOKING' }] }),
    true,
  );
  assert.equal(
    canCustomerRequestPayment({ items: [{ status: 'FINISHED' }, { status: 'CANCELLED' }] }),
    true,
  );
  assert.equal(
    canCustomerRequestPayment({ items: [{ status: 'FINISHED' }, { status: 'PENDING' }] }),
    false,
  );
  assert.equal(
    canCustomerRequestPayment({ items: [{ status: 'CANCELLED' }] }),
    false,
  );
  assert.equal(canCustomerRequestPayment({ items: [] }), false);
});

test('getSelfCheckoutStorageKey scopes by table code and order id', () => {
  assert.equal(getSelfCheckoutStorageKey('T01', 42), 'qros:self-checkout:T01:42');
  assert.equal(getSelfCheckoutStorageKey('', 42), null);
});

test('createSelfCheckoutWithRetry retries CREATING responses using the same idempotency key', async () => {
  const sentPayloads = [];
  const responses = [
    { transactionId: 1, status: 'CREATING' },
    { transactionId: 1, status: 'CREATING' },
    { transactionId: 1, status: 'PENDING', qrCode: 'vietqr' },
  ];

  const result = await createSelfCheckoutWithRetry({
    createFn: async (_tableCode, payload) => {
      sentPayloads.push(payload);
      return responses[sentPayloads.length - 1];
    },
    tableCode: 'T01',
    sessionToken: 'tok-1',
    voucherCode: 'SALE10',
    idempotencyKey: 'self-idem-1',
    waitForRetry: async () => {},
  });

  assert.equal(result.status, 'PENDING');
  assert.equal(sentPayloads.length, 3);
  assert.deepEqual(
    new Set(sentPayloads.map((p) => p.idempotencyKey)),
    new Set(['self-idem-1']),
  );
});
