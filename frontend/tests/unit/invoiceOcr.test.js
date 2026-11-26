import assert from 'node:assert/strict';
import test from 'node:test';

import {
  buildBatchStockInPayload,
  convertInvoiceUnit,
  createEmptyReviewRow,
  mapOcrResponseToReviewRows,
  MAX_BATCH_STOCK_IN_LINES,
  MAX_INVOICE_IMAGE_BYTES,
  recalculateRowForTargetItem,
  validateInvoiceImageFile,
} from '../../src/features/inventory-management/lib/invoiceOcrHelpers.js';

const CATALOG = [
  { id: 1, name: 'Thịt bò Úc', unit: 'g', unitCost: 220 },
  { id: 2, name: 'Sữa tươi không đường', unit: 'ml', unitCost: 35 },
  { id: 3, name: 'Hạt tiêu đen', unit: 'kg', unitCost: 190000 },
  { id: 4, name: 'Nước mắm', unit: 'l', unitCost: 48000 },
];

test('validateInvoiceImageFile accepts supported image formats within 5 MB', () => {
  for (const type of ['image/jpeg', 'image/png', 'image/webp']) {
    const res = validateInvoiceImageFile({ type, size: 1024 * 500 }, 'vi');
    assert.equal(res.valid, true);
    assert.equal(res.error, '');
  }
});

test('validateInvoiceImageFile rejects missing, empty, invalid MIME, and oversized files in vi and en', () => {
  const missingVi = validateInvoiceImageFile(null, 'vi');
  assert.equal(missingVi.valid, false);
  assert.match(missingVi.error, /chọn hoặc chụp/i);

  const missingEn = validateInvoiceImageFile(null, 'en');
  assert.equal(missingEn.valid, false);
  assert.match(missingEn.error, /choose or capture/i);

  const badMimeVi = validateInvoiceImageFile({ type: 'application/pdf', size: 1024 }, 'vi');
  assert.equal(badMimeVi.valid, false);
  assert.match(badMimeVi.error, /JPEG, PNG/);

  const badMimeEn = validateInvoiceImageFile({ type: 'application/pdf', size: 1024 }, 'en');
  assert.equal(badMimeEn.valid, false);
  assert.match(badMimeEn.error, /JPEG, PNG/);

  const emptyFile = validateInvoiceImageFile({ type: 'image/png', size: 0 }, 'en');
  assert.equal(emptyFile.valid, false);
  assert.match(emptyFile.error, /empty/i);

  const tooBigVi = validateInvoiceImageFile(
    { type: 'image/jpeg', size: MAX_INVOICE_IMAGE_BYTES + 1 },
    'vi',
  );
  assert.equal(tooBigVi.valid, false);
  assert.match(tooBigVi.error, /5 MB/);

  const tooBigEn = validateInvoiceImageFile(
    { type: 'image/webp', size: MAX_INVOICE_IMAGE_BYTES + 1 },
    'en',
  );
  assert.equal(tooBigEn.valid, false);
  assert.match(tooBigEn.error, /5 MB/);
});

test('convertInvoiceUnit converts kg<->g, l<->ml, aliases, identical, and incompatible units', () => {
  const kgToG = convertInvoiceUnit(2.5, 200000, 'kilogram', 'g', 'vi');
  assert.equal(kgToG.convertedQuantity, 2500);
  assert.equal(kgToG.convertedUnitCost, 200);
  assert.equal(kgToG.unitConverted, true);
  assert.equal(kgToG.unitMismatch, false);
  assert.match(kgToG.note, /2500 g/);

  const gToKg = convertInvoiceUnit(500, 180, 'gam', 'kg', 'en');
  assert.equal(gToKg.convertedQuantity, 0.5);
  assert.equal(gToKg.convertedUnitCost, 180000);
  assert.equal(gToKg.unitConverted, true);
  assert.equal(gToKg.unitMismatch, false);

  const lToMl = convertInvoiceUnit(1.5, 36000, 'lít', 'ml', 'vi');
  assert.equal(lToMl.convertedQuantity, 1500);
  assert.equal(lToMl.convertedUnitCost, 36);
  assert.equal(lToMl.unitConverted, true);
  assert.equal(lToMl.unitMismatch, false);

  const mlToL = convertInvoiceUnit(750, 50, 'cc', 'l', 'en');
  assert.equal(mlToL.convertedQuantity, 0.75);
  assert.equal(mlToL.convertedUnitCost, 50000);
  assert.equal(mlToL.unitConverted, true);
  assert.equal(mlToL.unitMismatch, false);

  const sameUnit = convertInvoiceUnit(3, 185000, 'kgs', 'kg', 'en');
  assert.equal(sameUnit.convertedQuantity, 3);
  assert.equal(sameUnit.convertedUnitCost, 185000);
  assert.equal(sameUnit.unitConverted, false);
  assert.equal(sameUnit.unitMismatch, false);

  const mismatch = convertInvoiceUnit(2, 45000, 'chai', 'kg', 'vi');
  assert.equal(mismatch.convertedQuantity, 2);
  assert.equal(mismatch.convertedUnitCost, 45000);
  assert.equal(mismatch.unitConverted, false);
  assert.equal(mismatch.unitMismatch, true);
  assert.match(mismatch.note, /chai/);
});

test('mapOcrResponseToReviewRows maps matched and unmatched lines with safe defaults', () => {
  const rows = mapOcrResponseToReviewRows(
    {
      lines: [
        {
          rawName: 'Thit bo Uc',
          invoiceQuantity: 2,
          invoiceUnit: 'kg',
          invoiceUnitPrice: 240000,
          matchedInventoryItemId: 1,
          matchedInventoryUnit: 'g',
          convertedQuantity: 2000,
          convertedUnitCost: 240,
          unitConverted: true,
          unitMismatch: false,
          matchStatus: 'MATCHED',
          confidence: 'HIGH',
          conversionNote: 'Tự quy đổi 2 kg -> 2000 g',
        },
        {
          rawName: 'Gia vi la',
          invoiceQuantity: 1,
          invoiceUnit: 'goi',
          invoiceUnitPrice: 15000,
          matchedInventoryItemId: null,
          matchStatus: 'UNMATCHED',
          confidence: 'LOW',
          conversionNote: 'Chưa khớp trong danh mục kho',
        },
      ],
    },
    CATALOG,
  );

  assert.equal(rows.length, 2);
  assert.equal(rows[0].inventoryItemId, '1');
  assert.equal(rows[0].quantity, '2000');
  assert.equal(rows[0].unitCost, '240');
  assert.equal(rows[0].currentUnitCost, 220);
  assert.equal(rows[0].updateUnitCost, false);
  assert.equal(rows[0].selected, true);

  assert.equal(rows[1].inventoryItemId, '');
  assert.equal(rows[1].selected, false);
  assert.equal(rows[1].updateUnitCost, false);
  assert.equal(rows[1].matchStatus, 'UNMATCHED');
});

test('recalculateRowForTargetItem updates quantity, unitCost, mismatch state, and manual rows', () => {
  const ocrRow = {
    ...createEmptyReviewRow(),
    rawName: 'Sua tuoi',
    invoiceQuantity: '2',
    invoiceUnit: 'lit',
    invoiceUnitPrice: '34000',
    quantity: '2',
    selected: false,
  };

  const matchedConvertible = recalculateRowForTargetItem(ocrRow, '2', CATALOG, 'en');
  assert.equal(matchedConvertible.inventoryItemId, '2');
  assert.equal(matchedConvertible.unit, 'ml');
  assert.equal(matchedConvertible.quantity, '2000');
  assert.equal(matchedConvertible.unitCost, '34');
  assert.equal(matchedConvertible.unitConverted, true);
  assert.equal(matchedConvertible.unitMismatch, false);
  assert.equal(matchedConvertible.matchStatus, 'MATCHED');
  assert.equal(matchedConvertible.selected, true);

  const matchedMismatch = recalculateRowForTargetItem(matchedConvertible, '3', CATALOG, 'en');
  assert.equal(matchedMismatch.inventoryItemId, '3');
  assert.equal(matchedMismatch.unit, 'kg');
  assert.equal(matchedMismatch.unitConverted, false);
  assert.equal(matchedMismatch.unitMismatch, true);
  assert.equal(matchedMismatch.matchStatus, 'UNIT_MISMATCH');
  assert.match(matchedMismatch.conversionNote, /differs from stock unit 'kg'/);

  const manualRow = {
    ...createEmptyReviewRow(),
    quantity: '5',
    unitCost: '48000',
  };
  const manualMatched = recalculateRowForTargetItem(manualRow, '4', CATALOG, 'vi');
  assert.equal(manualMatched.inventoryItemId, '4');
  assert.equal(manualMatched.unit, 'l');
  assert.equal(manualMatched.quantity, '5');
  assert.equal(manualMatched.unitMismatch, false);
  assert.equal(manualMatched.matchStatus, 'MATCHED');
});

test('buildBatchStockInPayload validates rows and enforces manager-only unit cost updates', () => {
  const emptyRes = buildBatchStockInPayload({}, [{ selected: false }], true, 'en');
  assert.equal(emptyRes.valid, false);

  const tooManyRows = Array.from({ length: MAX_BATCH_STOCK_IN_LINES + 1 }, (_, i) => ({
    selected: true,
    inventoryItemId: String(i + 1),
    quantity: '1',
    rawName: `Item ${i + 1}`,
  }));
  const overflowRes = buildBatchStockInPayload({}, tooManyRows, true, 'en');
  assert.equal(overflowRes.valid, false);
  assert.match(overflowRes.error, /50/);

  const unmatchedRes = buildBatchStockInPayload(
    {},
    [{ selected: true, inventoryItemId: '', quantity: '1', rawName: 'Bia' }],
    true,
    'vi',
  );
  assert.equal(unmatchedRes.valid, false);

  const badQtyRes = buildBatchStockInPayload(
    {},
    [{ selected: true, inventoryItemId: '1', quantity: '0', rawName: 'Thịt bò' }],
    true,
    'en',
  );
  assert.equal(badQtyRes.valid, false);

  const sampleRows = [
    {
      selected: true,
      inventoryItemId: '1',
      quantity: '2500',
      unitCost: '180000',
      updateUnitCost: true,
      note: '   Fresh cut   ',
      rawName: 'Thịt bò',
    },
    {
      selected: false,
      inventoryItemId: '2',
      quantity: '100',
      unitCost: '50',
      updateUnitCost: true,
      rawName: 'Skipped',
    },
  ];

  const chefRes = buildBatchStockInPayload(
    { supplierName: '  NCC A  ', invoiceRef: '  HD-01 ', note: '  Note  ' },
    sampleRows,
    false,
    'vi',
  );
  assert.equal(chefRes.valid, true);
  assert.equal(chefRes.payload.items.length, 1);
  assert.equal(chefRes.payload.items[0].unitCost, null);
  assert.equal(chefRes.payload.items[0].updateUnitCost, false);
  assert.equal(chefRes.payload.supplierName, 'NCC A');
  assert.equal(chefRes.payload.invoiceRef, 'HD-01');
  assert.equal(chefRes.payload.note, 'Note');

  const managerRes = buildBatchStockInPayload(
    { supplierName: '  NCC A  ', invoiceRef: '  HD-01 ', note: '' },
    sampleRows,
    true,
    'vi',
  );
  assert.equal(managerRes.valid, true);
  assert.equal(managerRes.payload.items[0].unitCost, 180000);
  assert.equal(managerRes.payload.items[0].updateUnitCost, true);
  assert.equal(managerRes.payload.items[0].note, 'Fresh cut');
  assert.equal(managerRes.payload.note, null);
});
