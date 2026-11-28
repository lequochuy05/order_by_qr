import assert from 'node:assert/strict';
import test from 'node:test';

import {
  BILLING_CYCLES,
  formatBillingCycleLabel,
  getFeaturePaywallCopy,
  getRemainingDays,
  getUsagePercent,
  hasPlanFeature,
  isPlanLimitReached,
  isProActive,
  PLAN_FEATURES,
} from '../../src/entities/subscription/lib/subscriptionHelpers.js';

test('isProActive and hasPlanFeature evaluate FREE, PRO, and enforcement-disabled states', () => {
  const freeSub = {
    planTier: 'FREE',
    status: 'ACTIVE',
    proActive: false,
    enforcementEnabled: true,
    enabledFeatures: [],
  };
  assert.equal(isProActive(freeSub), false);
  assert.equal(hasPlanFeature(freeSub, PLAN_FEATURES.AI_OPERATIONS_SUITE), false);
  assert.equal(hasPlanFeature(freeSub, PLAN_FEATURES.AI_CUSTOMER_ASSISTANT), false);

  const proSub = {
    planTier: 'PRO',
    status: 'ACTIVE',
    proActive: true,
    enforcementEnabled: true,
    enabledFeatures: [
      PLAN_FEATURES.AI_OPERATIONS_SUITE,
      PLAN_FEATURES.AI_CUSTOMER_ASSISTANT,
      PLAN_FEATURES.UNLIMITED_RESOURCES,
    ],
  };
  assert.equal(isProActive(proSub), true);
  assert.equal(hasPlanFeature(proSub, PLAN_FEATURES.AI_OPERATIONS_SUITE), true);
  assert.equal(hasPlanFeature(proSub, PLAN_FEATURES.AI_CUSTOMER_ASSISTANT), true);

  const bypassSub = {
    planTier: 'FREE',
    proActive: false,
    enforcementEnabled: false,
    enabledFeatures: [],
  };
  assert.equal(isProActive(bypassSub), true);
  assert.equal(hasPlanFeature(bypassSub, PLAN_FEATURES.AI_OPERATIONS_SUITE), true);
});

test('getRemainingDays calculates positive, zero, and null expiry cleanly', () => {
  const nowMs = Date.parse('2026-11-27T12:00:00Z');
  assert.equal(getRemainingDays(null, nowMs), null);
  assert.equal(getRemainingDays('2026-11-26T12:00:00Z', nowMs), 0);
  assert.equal(getRemainingDays('2026-12-11T12:00:00Z', nowMs), 14);
});

test('usage meters and plan limits compute percentages and caps accurately', () => {
  assert.equal(getUsagePercent(5, 10), 50);
  assert.equal(getUsagePercent(15, 10), 100);
  assert.equal(getUsagePercent(4, null), 0);
  assert.equal(isPlanLimitReached(10, 10), true);
  assert.equal(isPlanLimitReached(9, 10), false);
  assert.equal(isPlanLimitReached(25, null), false);
});

test('formatBillingCycleLabel and getFeaturePaywallCopy return localized VI and EN copy', () => {
  assert.equal(formatBillingCycleLabel(BILLING_CYCLES.MONTHLY, 'vi'), 'Pro Tháng (30 ngày)');
  assert.equal(formatBillingCycleLabel(BILLING_CYCLES.YEARLY, 'en'), 'Pro Yearly (365 days)');
  assert.equal(formatBillingCycleLabel(BILLING_CYCLES.TRIAL, 'vi'), 'Dùng thử 14 ngày');

  const limitsCopyVi = getFeaturePaywallCopy(PLAN_FEATURES.UNLIMITED_RESOURCES, 'vi');
  assert.match(limitsCopyVi.title, /Bàn & Nhân sự/);
  const aiCopyEn = getFeaturePaywallCopy(PLAN_FEATURES.AI_OPERATIONS_SUITE, 'en');
  assert.match(aiCopyEn.title, /AI Operations Suite/);
});
