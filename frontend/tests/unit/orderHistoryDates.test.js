import assert from 'node:assert/strict';
import test from 'node:test';

import {
  DATE_PRESETS,
  getOrderHistoryDateRange,
} from '../../src/features/order-management/lib/orderHistoryDates.js';

const TODAY = new Date(2026, 5, 20, 12, 0, 0);

test('order history exposes expected date presets', () => {
  assert.deepEqual(
    DATE_PRESETS.map((preset) => preset.value),
    ['all', 'today', 'yesterday', '7days', '30days'],
  );
});

test('today range uses the same local date for both ends', () => {
  assert.deepEqual(getOrderHistoryDateRange('today', TODAY), {
    startDate: '2026-06-20',
    endDate: '2026-06-20',
  });
});

test('yesterday range subtracts one calendar day', () => {
  assert.deepEqual(getOrderHistoryDateRange('yesterday', TODAY), {
    startDate: '2026-06-19',
    endDate: '2026-06-19',
  });
});

test('seven day range includes today', () => {
  assert.deepEqual(getOrderHistoryDateRange('7days', TODAY), {
    startDate: '2026-06-14',
    endDate: '2026-06-20',
  });
});

test('thirty day range includes today', () => {
  assert.deepEqual(getOrderHistoryDateRange('30days', TODAY), {
    startDate: '2026-05-22',
    endDate: '2026-06-20',
  });
});

test('all range has no date boundaries', () => {
  assert.deepEqual(getOrderHistoryDateRange('all', TODAY), {
    startDate: '',
    endDate: '',
  });
});

test('date range correctly crosses a year boundary', () => {
  assert.deepEqual(getOrderHistoryDateRange('7days', new Date(2026, 0, 3, 12, 0, 0)), {
    startDate: '2025-12-28',
    endDate: '2026-01-03',
  });
});
