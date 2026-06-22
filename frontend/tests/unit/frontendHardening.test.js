import assert from 'node:assert/strict';
import test from 'node:test';

import { canSubscribeToOperations, isCustomerMenuPath } from '../../src/app/webSocketAccess.js';
import { calculatePayosTimeLeft } from '../../src/features/payment/lib/paymentExpiry.js';
import { createPaymentWithRetry } from '../../src/features/payment/lib/paymentRetry.js';

test('operation WebSocket topics require an authenticated admin route', () => {
  const user = { role: 'MANAGER' };

  assert.equal(canSubscribeToOperations(user, '/admin/tables'), true);
  assert.equal(canSubscribeToOperations(user, '/menu'), false);
  assert.equal(canSubscribeToOperations(null, '/admin/tables'), false);
});

test('customer menu path detection excludes admin menu', () => {
  assert.equal(isCustomerMenuPath('/menu'), true);
  assert.equal(isCustomerMenuPath('/T01/menu'), true);
  assert.equal(isCustomerMenuPath('/T01/menu/'), true);
  assert.equal(isCustomerMenuPath('/admin/menu'), false);
  assert.equal(isCustomerMenuPath('/admin/dashboard'), false);
});

test('PayOS retries reuse one idempotency key', async () => {
  const payloads = [];
  const responses = [{ status: 'CREATING' }, { status: 'CREATING' }, { status: 'PENDING' }];

  const result = await createPaymentWithRetry({
    post: async (_url, payload) => {
      payloads.push(payload);
      return responses[payloads.length - 1];
    },
    orderId: 42,
    paymentMethod: 'PAYOS',
    voucherCode: 'SAVE10',
    idempotencyKey: 'payment-attempt-1',
    waitForRetry: async () => {},
  });

  assert.equal(result.status, 'PENDING');
  assert.equal(payloads.length, 3);
  assert.deepEqual(
    new Set(payloads.map((payload) => payload.idempotencyKey)),
    new Set(['payment-attempt-1']),
  );
});

test('cash payment also sends an idempotency key', async () => {
  let sentPayload;

  await createPaymentWithRetry({
    post: async (_url, payload) => {
      sentPayload = payload;
      return { status: 'PAID' };
    },
    orderId: 7,
    paymentMethod: 'CASH',
    idempotencyKey: 'cash-attempt-1',
  });

  assert.equal(sentPayload.idempotencyKey, 'cash-attempt-1');
});

test('PayOS countdown uses backend expiresAt', () => {
  const now = Date.parse('2026-06-22T10:00:00.000Z');

  assert.equal(calculatePayosTimeLeft('2026-06-22T10:02:00.000Z', now), 120);
  assert.equal(calculatePayosTimeLeft('2026-06-22T09:59:00.000Z', now), 0);
  assert.equal(calculatePayosTimeLeft('invalid', now), 0);
});
