import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

import { canSubscribeToOperations, isCustomerMenuPath } from '../../src/app/webSocketAccess.js';
import { calculatePayosTimeLeft } from '../../src/entities/payment/lib/paymentExpiry.js';
import { createPaymentWithRetry } from '../../src/entities/payment/lib/paymentRetry.js';
import {
  PAYMENT_METHODS,
  PAYMENT_TRANSACTION_STATUSES,
} from '../../src/entities/payment/lib/paymentStatus.js';

const testDir = dirname(fileURLToPath(import.meta.url));

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
  const responses = [
    { status: PAYMENT_TRANSACTION_STATUSES.CREATING },
    { status: PAYMENT_TRANSACTION_STATUSES.CREATING },
    { status: PAYMENT_TRANSACTION_STATUSES.PENDING },
  ];

  const result = await createPaymentWithRetry({
    post: async (_url, payload) => {
      payloads.push(payload);
      return responses[payloads.length - 1];
    },
    orderId: 42,
    paymentMethod: PAYMENT_METHODS.PAYOS,
    voucherCode: 'SAVE10',
    idempotencyKey: 'payment-attempt-1',
    waitForRetry: async () => {},
  });

  assert.equal(result.status, PAYMENT_TRANSACTION_STATUSES.PENDING);
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
      return { status: PAYMENT_TRANSACTION_STATUSES.PAID };
    },
    orderId: 7,
    paymentMethod: PAYMENT_METHODS.CASH,
    idempotencyKey: 'cash-attempt-1',
  });

  assert.equal(sentPayload.idempotencyKey, 'cash-attempt-1');
});

test('cash payment sends cashReceived, changeAmount, and confirmUnfinishedItems when provided', async () => {
  let sentPayload;

  await createPaymentWithRetry({
    post: async (_url, payload) => {
      sentPayload = payload;
      return { status: PAYMENT_TRANSACTION_STATUSES.PAID };
    },
    orderId: 8,
    paymentMethod: PAYMENT_METHODS.CASH,
    idempotencyKey: 'cash-attempt-2',
    cashReceived: 200000,
    changeAmount: 65000,
    confirmUnfinishedItems: true,
  });

  assert.equal(sentPayload.cashReceived, 200000);
  assert.equal(sentPayload.changeAmount, 65000);
  assert.equal(sentPayload.confirmUnfinishedItems, true);
});

test('PayOS countdown uses backend expiresAt', () => {
  const now = Date.parse('2026-06-22T10:00:00.000Z');

  assert.equal(calculatePayosTimeLeft('2026-06-22T10:02:00.000Z', now), 120);
  assert.equal(calculatePayosTimeLeft('2026-06-22T09:59:00.000Z', now), 0);
  assert.equal(calculatePayosTimeLeft('invalid', now), 0);
});

test('SharedModal focus trap is stable across onClose callback rerenders', () => {
  const source = readFileSync(resolve(testDir, '../../src/shared/ui/SharedModal.jsx'), 'utf8');
  const focusEffectDependencies = source.match(
    /document\.addEventListener\('keydown', handleKeyDown\);[\s\S]*?\n\s{2}}, \[([^\]]*)\]\);/,
  );

  assert.match(source, /const onCloseRef = useRef\(onClose\);/);
  assert.match(source, /useEffect\(\(\) => \{\n\s{4}onCloseRef\.current = onClose;/);
  assert.equal(focusEffectDependencies?.[1].trim(), 'isOpen');
});
