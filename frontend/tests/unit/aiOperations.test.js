import assert from 'node:assert/strict';
import test from 'node:test';

import {
  buildStockInPreset,
  findLiveInventoryTarget,
} from '../../src/features/inventory-management/lib/aiRestockHelpers.js';
import {
  calculateLocalBomAnalysis,
  mapSuggestedItemsToRecipeRows,
} from '../../src/features/inventory-management/lib/bomCostHelpers.js';
import {
  comboProposalToDraft,
  promotionProposalToDraft,
} from '../../src/entities/ai/lib/aiMerchandisingDrafts.js';

test('aiRestockHelpers resolves live target and builds StockModal preset', () => {
  const liveItems = [{ id: 10, name: 'Thịt bò Úc', unit: 'kg', quantityOnHand: 1.5, unitCost: 220000 }];
  const proposal = {
    inventoryItemId: 10,
    name: 'Thịt bò Úc',
    unit: 'kg',
    availableQuantity: 0.5,
    suggestedRestockQty: 4.25,
    reason: 'Chỉ còn đủ 1 ngày bán',
  };

  assert.equal(findLiveInventoryTarget(liveItems, proposal), liveItems[0]);

  const fallbackTarget = findLiveInventoryTarget([], proposal);
  assert.equal(fallbackTarget.id, 10);
  assert.equal(fallbackTarget.quantityOnHand, 0.5);

  const preset = buildStockInPreset(proposal, 'vi');
  assert.equal(preset.quantity, '4.25');
  assert.ok(preset.note.includes('Chỉ còn đủ 1 ngày bán'));
});

test('calculateLocalBomAnalysis evaluates NO_RECIPE, MISSING_COST, HEALTHY, WARNING, and CRITICAL', () => {
  const inventory = [
    { id: 1, unitCost: 0 },
    { id: 2, unitCost: 100000 },
    { id: 3, unitCost: 250000 },
  ];

  assert.equal(calculateLocalBomAnalysis(50000, [], inventory).marginStatus, 'NO_RECIPE');
  assert.equal(
    calculateLocalBomAnalysis(50000, [{ inventoryItemId: 1, quantityRequired: 0.2 }], inventory).marginStatus,
    'MISSING_COST',
  );

  const healthy = calculateLocalBomAnalysis(
    50000,
    [{ inventoryItemId: 2, quantityRequired: 0.15 }],
    inventory,
  );
  assert.equal(healthy.totalFoodCost, 15000);
  assert.equal(healthy.foodCostPercent, 30);
  assert.equal(healthy.grossProfit, 35000);
  assert.equal(healthy.marginStatus, 'HEALTHY');

  const warning = calculateLocalBomAnalysis(
    50000,
    [{ inventoryItemId: 2, quantityRequired: 0.2 }],
    inventory,
  );
  assert.equal(warning.foodCostPercent, 40);
  assert.equal(warning.marginStatus, 'WARNING');

  const critical = calculateLocalBomAnalysis(
    50000,
    [{ inventoryItemId: 3, quantityRequired: 0.1 }],
    inventory,
  );
  assert.equal(critical.totalFoodCost, 25000);
  assert.equal(critical.foodCostPercent, 50);
  assert.equal(critical.recommendedMinPrice, 72000);
  assert.equal(critical.marginStatus, 'CRITICAL');
});

test('mapSuggestedItemsToRecipeRows normalizes AI recipe lines for form state', () => {
  const rows = mapSuggestedItemsToRecipeRows([
    { inventoryItemId: 7, quantityRequired: 0.125 },
    { inventoryItemId: 0, quantityRequired: 1 },
  ]);
  assert.deepEqual(rows, [{ inventoryItemId: '7', quantityRequired: '0.125' }]);
});

test('comboProposalToDraft and promotionProposalToDraft produce create drafts without id', () => {
  const comboDraft = comboProposalToDraft(
    {
      name: 'Combo Trưa',
      description: 'Món chính + nước',
      suggestedPrice: 69000,
      items: [
        { menuItemId: 1, quantity: 1 },
        { menuItemId: 2, quantity: 2 },
      ],
    },
    0,
  );
  assert.equal(comboDraft.id, undefined);
  assert.ok(comboDraft._draftKey.startsWith('ai-combo-0-'));
  assert.equal(comboDraft.price, 69000);
  assert.equal(comboDraft.items.length, 2);

  const promoDraft = promotionProposalToDraft(
    {
      name: 'Happy Hour',
      discountPercent: 75,
      startTime: '14:00',
      endTime: '17:00',
      daysOfWeek: ['MONDAY', 'WEDNESDAY'],
    },
    1,
  );
  assert.equal(promoDraft.id, undefined);
  assert.ok(promoDraft._draftKey.startsWith('ai-promo-1-'));
  assert.equal(promoDraft.discountPercent, 40);
  assert.deepEqual(promoDraft.daysOfWeek, ['MONDAY', 'WEDNESDAY']);
});
