import assert from 'node:assert/strict';
import test from 'node:test';

import {
  getOrderDiscountAmount,
  getOrderFinalAmount,
  getOrderPaidAmount,
  getOrderSubtotalAmount,
} from '../../src/entities/order/lib/orderMoney.js';

test('order subtotal defaults to zero', () => {
  assert.equal(getOrderSubtotalAmount(null), 0);
});

test('order subtotal reads explicit value', () => {
  assert.equal(getOrderSubtotalAmount({ subtotalAmount: 120000 }), 120000);
});

test('order discount defaults to zero', () => {
  assert.equal(getOrderDiscountAmount({}), 0);
});

test('order final amount reads explicit zero without falling back', () => {
  assert.equal(getOrderFinalAmount({ finalAmount: 0, subtotalAmount: 100 }), 0);
});

test('order paid amount reads explicit paid value', () => {
  assert.equal(getOrderPaidAmount({ paidAmount: 50000, finalAmount: 90000 }), 50000);
});

test('order paid amount falls back to final amount', () => {
  assert.equal(getOrderPaidAmount({ finalAmount: 90000 }), 90000);
});
