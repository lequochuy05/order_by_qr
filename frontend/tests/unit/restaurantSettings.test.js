import assert from 'node:assert/strict';
import test from 'node:test';

import {
  getSessionStorageKey,
  getTimeContext,
  normalizeRestaurantSettings,
  translateRecommendationReason,
} from '../../src/features/customer-menu/lib/restaurantSettings.js';

test('restaurant settings provide safe defaults', () => {
  assert.deepEqual(normalizeRestaurantSettings(), {
    restaurantName: 'Sắc Màu Quán',
    restaurantPhone: '',
    logoUrl: '',
    orderingEnabled: true,
    maintenanceMode: false,
    enableAiAssistant: true,
    showRecommendations: true,
    showCombos: true,
    showUnavailableItems: false,
    onlinePaymentEnabled: true,
    customerSelfCheckoutEnabled: false,
    allowSettlementWithUnfinishedItems: false,
    currency: 'VND',
    taxPercent: 0,
    serviceChargePercent: 0,
  });
});

test('restaurant settings normalize the legacy logo property', () => {
  assert.equal(
    normalizeRestaurantSettings({ restaurantLogoUrl: '/legacy.png' }).logoUrl,
    '/legacy.png',
  );
});

test('restaurant settings preserve explicit disabled flags', () => {
  const settings = normalizeRestaurantSettings({
    orderingEnabled: false,
    enableAiAssistant: false,
    maintenanceMode: true,
    customerSelfCheckoutEnabled: true,
    onlinePaymentEnabled: false,
  });
  assert.equal(settings.orderingEnabled, false);
  assert.equal(settings.enableAiAssistant, false);
  assert.equal(settings.maintenanceMode, true);
  assert.equal(settings.customerSelfCheckoutEnabled, true);
  assert.equal(settings.onlinePaymentEnabled, false);
});

test('recommendation reasons are translated', () => {
  assert.equal(translateRecommendationReason('Popular items'), 'Món đang được gọi nhiều');
});

test('unknown recommendation reasons are preserved', () => {
  assert.equal(translateRecommendationReason('Chef choice'), 'Chef choice');
});

test('time context identifies morning', () => {
  assert.equal(getTimeContext(new Date(2026, 0, 1, 8)), 'Sáng');
});

test('time context identifies lunch', () => {
  assert.equal(getTimeContext(new Date(2026, 0, 1, 12)), 'Trưa');
});

test('time context identifies afternoon and evening', () => {
  assert.equal(getTimeContext(new Date(2026, 0, 1, 16)), 'Chiều');
  assert.equal(getTimeContext(new Date(2026, 0, 1, 20)), 'Tối');
});

test('session storage key is scoped by table code', () => {
  assert.equal(getSessionStorageKey('BAN_01'), 'qros:table-session:BAN_01');
});
