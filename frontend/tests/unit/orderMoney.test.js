import assert from 'node:assert/strict';
import test from 'node:test';

import { ORDER_ITEM_STATUSES } from '../../src/entities/order/lib/orderConstants.js';
import {
  calculateTaxAndServiceCharge,
  getBillableOrderItems,
  getOrderDiscountAmount,
  getOrderFinalAmount,
  getOrderPaidAmount,
  getOrderServiceChargeAmount,
  getOrderServiceChargePercent,
  getOrderSubtotalAmount,
  getOrderTaxAmount,
  getOrderTaxPercent,
  isBillableOrderItem,
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

test('cancelled order items are not billable', () => {
  assert.equal(isBillableOrderItem({ status: ORDER_ITEM_STATUSES.CANCELLED }), false);
});

test('billable order items exclude cancelled items', () => {
  const order = {
    orderItems: [
      { id: 1, status: ORDER_ITEM_STATUSES.FINISHED },
      { id: 2, status: ORDER_ITEM_STATUSES.CANCELLED },
      { id: 3, status: ORDER_ITEM_STATUSES.PENDING },
    ],
  };

  assert.deepEqual(
    getBillableOrderItems(order).map((item) => item.id),
    [1, 3],
  );
});

test('calculateTaxAndServiceCharge applies service charge then VAT matching backend formula', () => {
  const result = calculateTaxAndServiceCharge(100000, 20000, {
    serviceChargePercent: 5,
    taxPercent: 10,
  });

  assert.deepEqual(result, {
    subtotalAmount: 100000,
    discountAmount: 20000,
    serviceChargePercent: 5,
    serviceChargeAmount: 4000,
    taxPercent: 10,
    taxAmount: 8400,
    finalAmount: 92400,
  });

  assert.equal(getOrderServiceChargePercent({ serviceChargePercent: 5 }), 5);
  assert.equal(getOrderServiceChargeAmount({ serviceChargeAmount: 4000 }), 4000);
  assert.equal(getOrderTaxPercent({ taxPercent: 10 }), 10);
  assert.equal(getOrderTaxAmount({ taxAmount: 8400 }), 8400);
});
