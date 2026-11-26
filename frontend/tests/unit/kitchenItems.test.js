import assert from 'node:assert/strict';
import test from 'node:test';

import { ORDER_ITEM_STATUSES } from '../../src/entities/order/lib/orderConstants.js';
import {
  aggregateBatchCookingItems,
  buildKitchenItems,
  groupKitchenItems,
  KITCHEN_COLUMNS,
  OVERDUE_MINUTES,
  scaleRecipeItems,
  summarizeKitchenItems,
} from '../../src/features/kitchen-board/lib/kitchenItems.js';

const NOW = new Date('2026-06-20T12:00:00.000Z').getTime();

const orders = [
  {
    id: 10,
    createdAt: '2026-06-20T11:30:00.000Z',
    table: { tableNumber: 3 },
    orderItems: [
      {
        id: 1,
        status: ORDER_ITEM_STATUSES.PENDING,
        quantity: 2,
        createdAt: '2026-06-20T11:35:00.000Z',
        itemNameSnapshot: 'Cơm rang',
        notes: 'Ít cay',
        menuItem: { id: 101, category: { name: 'Cơm' }, available: false },
      },
      {
        id: 2,
        status: ORDER_ITEM_STATUSES.COOKING,
        quantity: 1,
        createdAt: '2026-06-20T11:50:00.000Z',
        updatedAt: '2026-06-20T11:55:00.000Z',
        combo: { id: 202, name: 'Combo trưa', available: true },
      },
      { id: 3, status: ORDER_ITEM_STATUSES.CANCELLED, quantity: 1 },
    ],
  },
];

test('kitchen columns cover all visible statuses', () => {
  assert.deepEqual(
    KITCHEN_COLUMNS.map((column) => column.status),
    [
      ORDER_ITEM_STATUSES.PENDING,
      ORDER_ITEM_STATUSES.COOKING,
      ORDER_ITEM_STATUSES.FINISHED,
    ],
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
    { id: 1, status: ORDER_ITEM_STATUSES.PENDING, waitMinutes: 4 },
    { id: 2, status: ORDER_ITEM_STATUSES.PENDING, waitMinutes: 12 },
  ]);
  assert.deepEqual(
    grouped.PENDING.map((item) => item.id),
    [2, 1],
  );
});

test('groupKitchenItems sorts finished items by latest update', () => {
  const grouped = groupKitchenItems([
    { id: 1, status: ORDER_ITEM_STATUSES.FINISHED, statusUpdatedAt: '2026-06-20T10:00:00Z' },
    { id: 2, status: ORDER_ITEM_STATUSES.FINISHED, statusUpdatedAt: '2026-06-20T11:00:00Z' },
  ]);
  assert.deepEqual(
    grouped.FINISHED.map((item) => item.id),
    [2, 1],
  );
});

test('summarizeKitchenItems calculates counts and average wait', () => {
  assert.deepEqual(
    summarizeKitchenItems([
      { status: ORDER_ITEM_STATUSES.PENDING, waitMinutes: 10, isOverdue: false },
      { status: ORDER_ITEM_STATUSES.COOKING, waitMinutes: 30, isOverdue: true },
      { status: ORDER_ITEM_STATUSES.FINISHED, waitMinutes: 40, isOverdue: false },
    ]),
    {
      pending: 1,
      cooking: 1,
      finished: 1,
      overdue: 1,
      allergenCount: 0,
      averageWait: 20,
    },
  );
});

test('buildKitchenItems and summarizeKitchenItems surface allergen risks and avoid homographs', () => {
  const allergyOrders = [
    {
      id: 99,
      createdAt: '2026-06-20T11:45:00.000Z',
      table: { tableNumber: 5 },
      orderItems: [
        {
          id: 501,
          status: ORDER_ITEM_STATUSES.PENDING,
          quantity: 1,
          createdAt: '2026-06-20T11:50:00.000Z',
          itemNameSnapshot: 'Gỏi Cuốn',
          notes: 'Dị ứng đậu phộng nặng',
        },
        {
          id: 502,
          status: ORDER_ITEM_STATUSES.PENDING,
          quantity: 1,
          createdAt: '2026-06-20T11:52:00.000Z',
          itemNameSnapshot: 'Cơm gà',
          notes: 'Bàn số 5 cần ghế trẻ em cho con của khách',
        },
      ],
    },
  ];

  const built = buildKitchenItems(allergyOrders, NOW);
  assert.equal(built[0].hasAllergenAlert, true);
  assert.equal(built[0].hasPriorityNote, true);
  assert.equal(built[1].hasAllergenAlert, false);
  assert.equal(built[1].hasPriorityNote, true);

  const summary = summarizeKitchenItems(built);
  assert.equal(summary.allergenCount, 1);
});

test('buildKitchenItems and aggregateBatchCookingItems preserve catalog IDs and availability', () => {
  const built = buildKitchenItems(orders, NOW);
  assert.equal(built[0].menuItemId, 101);
  assert.equal(built[0].comboId, null);
  assert.equal(built[0].isCombo, false);
  assert.equal(built[0].isAvailable, false);

  assert.equal(built[1].menuItemId, null);
  assert.equal(built[1].comboId, 202);
  assert.equal(built[1].isCombo, true);
  assert.equal(built[1].isAvailable, true);

  const batches = aggregateBatchCookingItems(built);
  assert.equal(batches.length, 2);
  const friedRiceBatch = batches.find((g) => g.itemName === 'Cơm rang');
  const comboBatch = batches.find((g) => g.itemName === 'Combo trưa');

  assert.equal(friedRiceBatch.menuItemId, 101);
  assert.equal(friedRiceBatch.isAvailable, false);
  assert.equal(comboBatch.comboId, 202);
  assert.equal(comboBatch.isCombo, true);
  assert.equal(comboBatch.isAvailable, true);
});

test('scaleRecipeItems scales ingredient quantities and flags stock shortages', () => {
  const scaled = scaleRecipeItems(
    [
      {
        id: 1,
        inventoryItemId: 10,
        inventoryItemName: 'Thịt bò',
        unit: 'kg',
        quantityRequired: 0.25,
        availableQuantity: 0.6,
        lowStock: true,
        outOfStock: false,
      },
      {
        id: 2,
        inventoryItemId: 11,
        inventoryItemName: 'Bún tươi',
        unit: 'kg',
        quantityRequired: 0.3,
        availableQuantity: 0,
        lowStock: true,
        outOfStock: true,
      },
      {
        id: 3,
        inventoryItemId: 12,
        inventoryItemName: 'Hành lá',
        unit: 'g',
        quantityRequired: 15,
      },
    ],
    3,
  );

  assert.equal(scaled[0].scaledQuantity, 0.75);
  assert.equal(scaled[0].isInsufficientStock, true);
  assert.equal(scaled[0].isOutOfStock, false);
  assert.equal(scaled[0].isLowStock, true);

  assert.equal(scaled[1].scaledQuantity, 0.9);
  assert.equal(scaled[1].isOutOfStock, true);
  assert.equal(scaled[1].isInsufficientStock, true);

  assert.equal(scaled[2].scaledQuantity, 45);
  assert.equal(scaled[2].hasStockInfo, false);
  assert.equal(scaled[2].isInsufficientStock, false);

  const clamped = scaleRecipeItems([{ id: 4, quantityRequired: 0.5 }], 0);
  assert.equal(clamped[0].scaledQuantity, 0.5);
});
