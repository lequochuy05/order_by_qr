import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

import {
  isPromotionActiveAt,
  selectBestActivePromotion,
} from '../../src/features/customer-menu/lib/selfCheckoutHelpers.js';
import {
  SETTINGS_FIELD_TO_TAB,
  resolveFirstSettingsErrorTab,
} from '../../src/features/settings-management/lib/settingsConstants.js';
import { isKitchenDisplayRoute } from '../../src/shared/lib/routeMatchers.js';

const testDir = dirname(fileURLToPath(import.meta.url));
const readSrc = (relativePath) => readFileSync(resolve(testDir, relativePath), 'utf8');

test('NEW-01 & NEW-03: KitchenCard removes broken FINISHED revert button and hides cancel on PAID orders', () => {
  const kitchenCard = readSrc('../../src/features/kitchen-board/ui/KitchenCard.jsx');
  assert.doesNotMatch(kitchenCard, /Revert to cooking/);
  assert.doesNotMatch(kitchenCard, /Hoàn tác về đang nấu/);
  assert.doesNotMatch(kitchenCard, /RotateCcw/);
  assert.match(kitchenCard, /const isOrderPaid = item\.orderPaymentStatus === 'PAID'/);
  assert.match(kitchenCard, /!isOrderPaid/);

  const kitchenItems = readSrc('../../src/features/kitchen-board/lib/kitchenItems.js');
  assert.match(kitchenItems, /orderPaymentStatus:\s*order\.paymentStatus/);

  const orderDetailModal = readSrc('../../src/widgets/order-modals/ui/OrderDetailModal.jsx');
  assert.match(orderDetailModal, /const isOrderPaid = order\?\.paymentStatus === 'PAID'/);
  assert.match(orderDetailModal, /\{!isOrderPaid && \(/);
});

test('NEW-06: OrderDetailsModal renders service charge, VAT rows, and cancelled item badges', () => {
  const orderDetails = readSrc('../../src/features/order-management/ui/OrderDetailsModal.jsx');
  assert.match(orderDetails, /getOrderServiceChargeAmount/);
  assert.match(orderDetails, /getOrderTaxAmount/);
  assert.match(orderDetails, /serviceChargeAmount > 0/);
  assert.match(orderDetails, /taxAmount > 0/);
  assert.match(orderDetails, /ORDER_ITEM_STATUSES\.CANCELLED/);
  assert.match(orderDetails, /line-through/);
});

test('NEW-09: CurrentOrderBanner forwards restaurantSettings into calculateSelfCheckoutBreakdown', () => {
  const customerMenu = readSrc('../../src/features/customer-menu/ui/CustomerMenu.jsx');
  assert.match(
    customerMenu,
    /<CurrentOrderBanner[\s\S]*?restaurantSettings=\{restaurantSettings\}/,
  );

  const banner = readSrc('../../src/features/customer-menu/ui/CurrentOrderBanner.jsx');
  assert.match(
    banner,
    /calculateSelfCheckoutBreakdown\(\s*order,\s*null,\s*activePromotion,\s*null,\s*restaurantSettings/,
  );
});

test('ISSUE-059: selectBestActivePromotion enforces weekday and time windows including overnight ranges', () => {
  const lunchPromo = {
    id: 1,
    name: 'Happy Hour Lunch',
    discountPercent: 15,
    active: true,
    startTime: '11:00:00',
    endTime: '14:00:00',
    daysOfWeek: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
  };
  const lateNightPromo = {
    id: 2,
    name: 'Late Night',
    discountPercent: 20,
    active: true,
    startTime: '22:00:00',
    endTime: '02:00:00',
    daysOfWeek: ['FRIDAY'],
  };

  // Monday 12:30 -> lunchPromo active
  assert.equal(isPromotionActiveAt(lunchPromo, new Date(2026, 4, 11, 12, 30, 0)), true);
  // Monday 15:00 -> expired
  assert.equal(isPromotionActiveAt(lunchPromo, new Date(2026, 4, 11, 15, 0, 0)), false);
  // Saturday 12:30 -> wrong day
  assert.equal(isPromotionActiveAt(lunchPromo, new Date(2026, 4, 16, 12, 30, 0)), false);

  // Friday 23:15 -> overnight promo active
  assert.equal(isPromotionActiveAt(lateNightPromo, new Date(2026, 4, 15, 23, 15, 0)), true);
  // Saturday 01:15 -> overnight tail of Friday promo still active
  assert.equal(isPromotionActiveAt(lateNightPromo, new Date(2026, 4, 16, 1, 15, 0)), true);
  // Saturday 02:30 -> expired
  assert.equal(isPromotionActiveAt(lateNightPromo, new Date(2026, 4, 16, 2, 30, 0)), false);

  const selected = selectBestActivePromotion(
    [lunchPromo, lateNightPromo],
    new Date(2026, 4, 15, 23, 15, 0),
  );
  assert.equal(selected?.id, 2);
  assert.equal(
    selectBestActivePromotion([lunchPromo, lateNightPromo], new Date(2026, 4, 16, 12, 0, 0)),
    null,
  );
});

test('ISSUE-059: customer session preserves QR signature on expiry and CartModal supports combo steppers & 255-char notes', () => {
  const sessionHook = readSrc('../../src/features/customer-menu/model/useCustomerMenuSession.js');
  const clearBlock = sessionHook.match(/const clearSessionToken = useCallback\(\(\) => \{[\s\S]*?\}, \[tableCode\]\);/);
  assert.ok(clearBlock, 'clearSessionToken block should exist');
  assert.match(clearBlock[0], /removeItem\(getSessionStorageKey\(tableCode\)\)/);
  assert.doesNotMatch(clearBlock[0], /removeItem\(getSigStorageKey\(tableCode\)\)/);

  const cartHook = readSrc('../../src/features/customer-menu/model/useCustomerCart.js');
  assert.match(cartHook, /handleUpdateCartComboQty/);
  assert.match(cartHook, /resolveLiveItemPrice/);

  const cartModal = readSrc('../../src/features/customer-menu/ui/CartModal.jsx');
  assert.match(cartModal, /handleUpdateCartComboQty\(id, combo\.qty - 1\)/);
  assert.match(cartModal, /handleUpdateCartComboQty\(id, combo\.qty \+ 1\)/);
  assert.equal((cartModal.match(/maxLength=\{255\}/g) || []).length, 2);
});

test('ISSUE-027, ISSUE-064, ISSUE-071, ISSUE-077: auth, WebSocket, combo min price, and settings multi-tab errors stay enforced', () => {
  assert.equal(isKitchenDisplayRoute('/admin/kitchen'), true);
  assert.equal(isKitchenDisplayRoute('/admin/kitchen/'), true);
  assert.equal(isKitchenDisplayRoute('/admin/tables'), false);

  const httpClient = readSrc('../../src/shared/api/httpClient.js');
  assert.match(httpClient, /registerAuthFailureListener/);
  assert.match(httpClient, /isAccessTokenExpiringSoon/);

  const websocket = readSrc('../../src/shared/lib/websocket.js');
  assert.match(websocket, /beforeConnect:\s*async\s*\(stompClient\)/);
  assert.match(websocket, /isAccessTokenExpiringSoon\(\)/);
  assert.match(websocket, /await refreshAccessToken\(\)/);

  const authContext = readSrc('../../src/entities/session/model/AuthContext.jsx');
  assert.match(authContext, /useState\(\(\) => isAdminRoute\(window\.location\.pathname\)\)/);
  assert.match(authContext, /registerAuthFailureListener\(/);
  assert.match(authContext, /isKitchenDisplayRoute\(window\.location\.pathname\)/);
  assert.match(authContext, /passive:\s*true/);
  assert.match(authContext, /useMemo\(/);

  const useWs = readSrc('../../src/shared/hooks/useWebSocket.js');
  assert.match(useWs, /try \{\s*subscriptionRef\.current\.unsubscribe\(\);/);

  const authService = readSrc('../../src/entities/session/api/authService.js');
  assert.match(authService, /\/auth\/forgot-password-email',\s*\{\s*email\s*\}/);

  const profileMgmt = readSrc('../../src/features/profile-management/ui/ProfileManagement.jsx');
  assert.match(profileMgmt, /\/\^\(84\|0\)\[0-9\]\{9\}\$\//);

  const comboModal = readSrc('../../src/features/combo-management/ui/ComboModal.jsx');
  assert.match(comboModal, /price < 1000/);
  assert.match(comboModal, /min="1000"/);

  assert.equal(SETTINGS_FIELD_TO_TAB.paymentQrExpiresInMinutes, 'payment');
  assert.equal(SETTINGS_FIELD_TO_TAB.validTimeRange, 'operation');
  assert.equal(SETTINGS_FIELD_TO_TAB.billFooterMessage, 'bill');
  assert.equal(SETTINGS_FIELD_TO_TAB.wifiName, 'wifi');
  assert.equal(resolveFirstSettingsErrorTab({ paymentQrExpiresInMinutes: 'Too high' }), 'payment');
  assert.equal(resolveFirstSettingsErrorTab({ validTimeRange: 'Invalid range' }), 'operation');
  assert.equal(resolveFirstSettingsErrorTab({}), null);
});

test('NEW-05 & ISSUE-032: PWA excludes admin/chart chunks and Capacitor uses dedicated build mode', () => {
  const viteConfig = readSrc('../../vite.config.js');
  assert.match(viteConfig, /globIgnores:/);
  assert.match(viteConfig, /vendor-charts/);
  assert.match(viteConfig, /lazy-assets-cache/);

  const pkg = JSON.parse(readSrc('../../package.json'));
  assert.match(pkg.scripts['build:capacitor'], /--mode capacitor/);
});
