import assert from 'node:assert/strict';
import test from 'node:test';

import {
  buildKitchenItems,
  groupKitchenItems,
  KITCHEN_COLUMNS,
  OVERDUE_MINUTES,
  summarizeKitchenItems,
} from '../../src/widgets/kitchen-board/lib/kitchenItems.js';

const NOW = new Date('2026-06-20T12:00:00.000Z').getTime();

const orders = [
  {
    id: 10,
    createdAt: '2026-06-20T11:30:00.000Z',
    table: { tableNumber: 3 },
    orderItems: [
      {
        id: 1,
        status: 'PENDING',
        quantity: 2,
        createdAt: '2026-06-20T11:35:00.000Z',
        itemNameSnapshot: 'Cơm rang',
        notes: 'Ít cay',
        menuItem: { category: { name: 'Cơm' } },
      },
      {
        id: 2,
        status: 'COOKING',
        quantity: 1,
        createdAt: '2026-06-20T11:50:00.000Z',
        updatedAt: '2026-06-20T11:55:00.000Z',
        combo: { name: 'Combo trưa' },
      },
      { id: 3, status: 'CANCELLED', quantity: 1 },
    ],
  },
];

test('kitchen columns cover all visible statuses', () => {
  assert.deepEqual(
    KITCHEN_COLUMNS.map((column) => column.status),
    ['PENDING', 'COOKING', 'FINISHED'],
  );
});

test('overdue threshold is twenty minutes', () => {
  assert.equal(OVERDUE_MINUTES, 20);
});

test('buildKitchenItems excludes unsupported statuses', () => {
  const items = buildKitchenItems(orders, NOW);
  assert.deepEqual(
    items.map((item) => item.id),
    [1, 2],
  );
});

test('buildKitchenItems derives names, table and category', () => {
  const [item] = buildKitchenItems(orders, NOW);
  assert.equal(item.itemName, 'Cơm rang');
  assert.equal(item.tableName, 3);
  assert.equal(item.category, 'Cơm');
  assert.equal(item.orderId, 10);
});

test('buildKitchenItems derives wait and note metadata', () => {
  const [item] = buildKitchenItems(orders, NOW);
  assert.equal(item.waitMinutes, 25);
  assert.equal(item.isOverdue, true);
  assert.equal(item.hasNotes, true);
});

test('combo items use Combo as their category', () => {
  const item = buildKitchenItems(orders, NOW)[1];
  assert.equal(item.itemName, 'Combo trưa');
  assert.equal(item.category, 'Combo');
  assert.equal(item.stageMinutes, 5);
});

test('groupKitchenItems sorts pending items by longest wait', () => {
  const grouped = groupKitchenItems([
    { id: 1, status: 'PENDING', waitMinutes: 4 },
    { id: 2, status: 'PENDING', waitMinutes: 12 },
  ]);
  assert.deepEqual(
    grouped.PENDING.map((item) => item.id),
    [2, 1],
  );
});

test('groupKitchenItems sorts finished items by latest update', () => {
  const grouped = groupKitchenItems([
    { id: 1, status: 'FINISHED', statusUpdatedAt: '2026-06-20T10:00:00Z' },
    { id: 2, status: 'FINISHED', statusUpdatedAt: '2026-06-20T11:00:00Z' },
  ]);
  assert.deepEqual(
    grouped.FINISHED.map((item) => item.id),
    [2, 1],
  );
});

test('summarizeKitchenItems calculates counts and average wait', () => {
  assert.deepEqual(
    summarizeKitchenItems([
      { status: 'PENDING', waitMinutes: 10, isOverdue: false },
      { status: 'COOKING', waitMinutes: 30, isOverdue: true },
      { status: 'FINISHED', waitMinutes: 40, isOverdue: false },
    ]),
    {
      pending: 1,
      cooking: 1,
      finished: 1,
      overdue: 1,
      averageWait: 20,
    },
  );
});
