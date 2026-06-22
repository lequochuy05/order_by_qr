import assert from 'node:assert/strict';
import test from 'node:test';

import {
  defaultInventoryForm,
  INVENTORY_PAGE_SIZE,
  INVENTORY_UNITS,
  movementLabels,
  STOCK_FILTER_OPTIONS,
} from '../../src/features/inventory-management/lib/inventoryConstants.js';
import {
  fmtQty,
  getInventoryUnitOptions,
  numberValue,
} from '../../src/features/inventory-management/lib/inventoryFormat.js';

test('numberValue converts empty values to zero', () => {
  assert.equal(numberValue(undefined), 0);
  assert.equal(numberValue(''), 0);
});

test('numberValue converts numeric strings', () => {
  assert.equal(numberValue('12.5'), 12.5);
});

test('fmtQty formats whole quantities in Vietnamese locale', () => {
  assert.equal(fmtQty(1200), '1.200');
});

test('fmtQty limits decimal quantities to three digits', () => {
  assert.equal(fmtQty(1.23456), '1,235');
});

test('inventory unit options return standard units unchanged', () => {
  assert.equal(getInventoryUnitOptions('kg'), INVENTORY_UNITS);
});

test('inventory unit options include a legacy custom unit', () => {
  assert.deepEqual(getInventoryUnitOptions('chai'), ['g', 'kg', 'ml', 'l', 'chai']);
});

test('default inventory form returns a fresh object', () => {
  const first = defaultInventoryForm();
  const second = defaultInventoryForm();
  assert.notEqual(first, second);
  assert.deepEqual(first, {
    name: '',
    unit: 'g',
    quantityOnHand: 0,
    lowStockThreshold: 0,
    active: true,
  });
});

test('inventory constants define pagination and filters', () => {
  assert.equal(INVENTORY_PAGE_SIZE, 24);
  assert.deepEqual(
    STOCK_FILTER_OPTIONS.map((option) => option.id),
    ['LOW', 'OUT', 'INACTIVE'],
  );
});

test('inventory movement labels cover stock operations', () => {
  assert.equal(movementLabels.STOCK_IN, 'Nhập kho');
  assert.equal(movementLabels.ADJUSTMENT, 'Kiểm kê');
  assert.equal(movementLabels.ORDER_RESERVE, 'Trừ theo đơn');
  assert.equal(movementLabels.ORDER_RELEASE, 'Hoàn theo đơn');
});
