import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

import {
  canSubscribeToKitchen,
  canSubscribeToMarketing,
  canSubscribeToOperations,
  canSubscribeToServiceRequests,
  canSubscribeToUsers,
  isCustomerMenuPath,
} from '../../src/app/webSocketAccess.js';
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

test('role-specific WebSocket subscriptions enforce role permissions', () => {
  const chef = { role: 'CHEF' };
  const manager = { role: 'MANAGER' };
  const cashier = { role: 'CASHIER' };

  // Users topic: only ADMIN, MANAGER
  assert.equal(canSubscribeToUsers(manager, '/admin/users'), true);
  assert.equal(canSubscribeToUsers(chef, '/admin/kitchen'), false);
  assert.equal(canSubscribeToUsers(cashier, '/admin/tables'), false);

  // Marketing (vouchers/promotions): ADMIN, MANAGER, CASHIER, STAFF (not CHEF)
  assert.equal(canSubscribeToMarketing(manager, '/admin/vouchers'), true);
  assert.equal(canSubscribeToMarketing(cashier, '/admin/vouchers'), true);
  assert.equal(canSubscribeToMarketing(chef, '/admin/kitchen'), false);

  // Kitchen: ADMIN, MANAGER, CHEF
  assert.equal(canSubscribeToKitchen(chef, '/admin/kitchen'), true);
  assert.equal(canSubscribeToKitchen(manager, '/admin/kitchen'), true);
  assert.equal(canSubscribeToKitchen(cashier, '/admin/tables'), false);

  // Service requests: ADMIN, MANAGER, CASHIER, STAFF (not CHEF, not /menu, not anonymous)
  assert.equal(canSubscribeToServiceRequests({ role: 'STAFF' }, '/admin/service-requests'), true);
  assert.equal(canSubscribeToServiceRequests(cashier, '/admin/tables'), true);
  assert.equal(canSubscribeToServiceRequests(manager, '/admin/tables'), true);
  assert.equal(canSubscribeToServiceRequests({ role: 'ADMIN' }, '/admin/tables'), true);
  assert.equal(canSubscribeToServiceRequests(chef, '/admin/kitchen'), false);
  assert.equal(canSubscribeToServiceRequests({ role: 'STAFF' }, '/menu'), false);
  assert.equal(canSubscribeToServiceRequests(null, '/admin/service-requests'), false);
});

test('staff role hides payment and prepared actions on management interfaces', () => {
  const tableCardSource = readFileSync(
    resolve(testDir, '../../src/features/table-management/ui/TableCard.jsx'),
    'utf-8',
  );
  assert.match(tableCardSource, /canPay/);
  assert.match(tableCardSource, /hasOrder\s*&&\s*!isOrderPaid\s*&&\s*canPay/);

  const orderDetailModalSource = readFileSync(
    resolve(testDir, '../../src/widgets/order-modals/ui/OrderDetailModal.jsx'),
    'utf-8',
  );
  assert.match(orderDetailModalSource, /isStaff/);
  assert.match(orderDetailModalSource, /!isStaff\s*&&\s*\(/);
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

test('customer realtime hook subscribes only to public WebSocket topics', () => {
  const source = readFileSync(
    resolve(testDir, '../../src/features/customer-menu/model/useCustomerMenuRealtime.js'),
    'utf8',
  );

  assert.doesNotMatch(source, /\/topic\/promotions/);
  assert.match(source, /\/topic\/menu/);
  assert.match(source, /\/topic\/tables/);
});

test('SystemErrorPage falls back to anchor tag when rendered outside a Router context', () => {
  const source = readFileSync(
    resolve(testDir, '../../src/shared/ui/SystemErrorPage.jsx'),
    'utf8',
  );

  assert.match(source, /useInRouterContext/);
  assert.match(source, /if \(!inRouter\)/);
  assert.match(source, /<a href=\{action\.to\}/);
});

test('core session and public catalog services retain required runtime methods', () => {
  const authSource = readFileSync(
    resolve(testDir, '../../src/entities/session/api/authService.js'),
    'utf8',
  );
  const publicMenuSource = readFileSync(
    resolve(testDir, '../../src/entities/public-menu/api/publicMenuService.js'),
    'utf8',
  );

  assert.match(authSource, /\brefresh\s*:/);
  assert.match(publicMenuSource, /\bgetCrossSellRecommendations\s*:/);
});

test('audit hardening regressions stay enforced across auth, inventory, POS, and customer checkout', () => {
  const authContextSource = readFileSync(
    resolve(testDir, '../../src/entities/session/model/AuthContext.jsx'),
    'utf8',
  );
  assert.match(authContextSource, /getAccessToken\(\)/);
  assert.match(authContextSource, /authService\.logout\(activeToken\)/);

  const inventoryFormSource = readFileSync(
    resolve(testDir, '../../src/features/inventory-management/model/useInventoryItemForm.js'),
    'utf8',
  );
  assert.doesNotMatch(inventoryFormSource, /inventoryService\.stockIn/);

  const paymentModalSource = readFileSync(
    resolve(testDir, '../../src/widgets/order-modals/ui/PaymentModal.jsx'),
    'utf8',
  );
  assert.ok(
    paymentModalSource.indexOf('<VoucherBox') <
      paymentModalSource.indexOf('payment.paymentMethod === PAYMENT_METHODS.PAYOS'),
    'VoucherBox must render above Cash/PayOS tab branch',
  );

  const customerMenuSource = readFileSync(
    resolve(testDir, '../../src/features/customer-menu/ui/CustomerMenu.jsx'),
    'utf8',
  );
  assert.match(customerMenuSource, /useCustomerSelfCheckout\(/);

  const sidebarSource = readFileSync(
    resolve(testDir, '../../src/widgets/admin-layout/ui/AdminSidebar.jsx'),
    'utf8',
  );
  assert.match(sidebarSource, /\/admin\/statistics\/revenue.*'MANAGER'/);
  assert.match(sidebarSource, /!user\.mustChangePassword/);
  assert.match(sidebarSource, /staleTime:/);

  const queryKeysSource = readFileSync(
    resolve(testDir, '../../src/shared/api/queryKeys.js'),
    'utf8',
  );
  assert.match(queryKeysSource, /filters\.orderId/);

  const invoiceSource = readFileSync(
    resolve(testDir, '../../src/entities/order/lib/invoiceGenerator.js'),
    'utf8',
  );
  assert.doesNotMatch(invoiceSource, /window\.open\(/);
  assert.match(invoiceSource, /createElement\('iframe'\)/);
});
