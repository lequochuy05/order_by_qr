export const MAX_INVOICE_IMAGE_BYTES = 5 * 1024 * 1024;
export const MAX_BATCH_STOCK_IN_LINES = 50;
const ALLOWED_INVOICE_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

const roundTo = (value, decimals) => {
  const num = Number(value);
  if (!Number.isFinite(num)) return 0;
  const factor = 10 ** decimals;
  return Math.round((num + Number.EPSILON) * factor) / factor;
};

const formatPlainNumber = (value) => {
  const num = Number(value);
  if (!Number.isFinite(num)) return '0';
  return String(num);
};

const normalizeUnitCode = (rawUnit) => {
  if (!rawUnit || typeof rawUnit !== 'string') return '';
  const folded = rawUnit
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[đĐ]/g, 'd')
    .replace(/\./g, '')
    .trim()
    .toLowerCase();

  switch (folded) {
    case 'kg':
    case 'kgs':
    case 'kilogram':
    case 'kilograms':
    case 'kilo':
    case 'ki':
    case 'ky':
      return 'kg';
    case 'g':
    case 'gr':
    case 'gram':
    case 'grams':
    case 'gam':
      return 'g';
    case 'l':
    case 'lit':
    case 'liter':
    case 'liters':
    case 'litre':
    case 'litres':
      return 'l';
    case 'ml':
    case 'mililit':
    case 'milliliter':
    case 'milliliters':
    case 'millilitre':
    case 'cc':
      return 'ml';
    default:
      return folded;
  }
};

export const validateInvoiceImageFile = (file, language = 'vi') => {
  const isEn = language === 'en';
  if (!file) {
    return {
      valid: false,
      error: isEn ? 'Please choose or capture an invoice photo.' : 'Vui lòng chọn hoặc chụp ảnh hóa đơn.',
    };
  }

  const mime = String(file.type || '').trim().toLowerCase();
  if (!ALLOWED_INVOICE_MIME_TYPES.includes(mime)) {
    return {
      valid: false,
      error: isEn
        ? 'Unsupported image format. Please use JPEG, PNG, or WebP.'
        : 'Định dạng ảnh không hỗ trợ. Vui lòng dùng JPEG, PNG hoặc WebP.',
    };
  }

  const size = Number(file.size || 0);
  if (size <= 0) {
    return {
      valid: false,
      error: isEn ? 'Selected image file is empty.' : 'Tệp ảnh được chọn bị rỗng.',
    };
  }

  if (size > MAX_INVOICE_IMAGE_BYTES) {
    return {
      valid: false,
      error: isEn
        ? 'Invoice image exceeds the 5 MB limit.'
        : 'Ảnh hóa đơn vượt quá giới hạn tối đa 5 MB.',
    };
  }

  return { valid: true, error: '' };
};

export const convertInvoiceUnit = (quantity, unitPrice, fromUnit, toUnit, language = 'vi') => {
  const isEn = language === 'en';
  const safeQty = roundTo(quantity, 3);
  const parsedPrice =
    unitPrice !== null && unitPrice !== undefined && unitPrice !== '' && Number.isFinite(Number(unitPrice))
      ? roundTo(unitPrice, 2)
      : null;

  const normFrom = normalizeUnitCode(fromUnit);
  const normTo = normalizeUnitCode(toUnit);

  if (!normTo || normFrom === normTo) {
    return {
      convertedQuantity: safeQty,
      convertedUnitCost: parsedPrice,
      unitConverted: false,
      unitMismatch: false,
      note: '',
    };
  }

  let factor = null;
  if (normFrom === 'kg' && normTo === 'g') {
    factor = 1000;
  } else if (normFrom === 'g' && normTo === 'kg') {
    factor = 0.001;
  } else if (normFrom === 'l' && normTo === 'ml') {
    factor = 1000;
  } else if (normFrom === 'ml' && normTo === 'l') {
    factor = 0.001;
  }

  if (factor !== null) {
    const convertedQuantity = roundTo(Number(quantity || 0) * factor, 3);
    const convertedUnitCost = parsedPrice !== null ? roundTo(parsedPrice / factor, 2) : null;
    const note = isEn
      ? `Auto-converted ${formatPlainNumber(safeQty)} ${fromUnit} -> ${formatPlainNumber(convertedQuantity)} ${toUnit}`
      : `Tự quy đổi ${formatPlainNumber(safeQty)} ${fromUnit} -> ${formatPlainNumber(convertedQuantity)} ${toUnit}`;
    return {
      convertedQuantity,
      convertedUnitCost,
      unitConverted: true,
      unitMismatch: false,
      note,
    };
  }

  const displayFrom = fromUnit && String(fromUnit).trim() ? fromUnit : '?';
  const note = isEn
    ? `Receipt unit '${displayFrom}' differs from stock unit '${toUnit}' — please verify converted quantity.`
    : `Đơn vị hóa đơn '${displayFrom}' khác đơn vị kho '${toUnit}' — hãy kiểm tra lại số lượng quy đổi.`;

  return {
    convertedQuantity: safeQty,
    convertedUnitCost: parsedPrice,
    unitConverted: false,
    unitMismatch: true,
    note,
  };
};

let rowSequence = 0;
const nextRowKey = () => {
  rowSequence += 1;
  return `ocr-row-${rowSequence}`;
};

export const createEmptyReviewRow = () => ({
  _key: nextRowKey(),
  rawName: '',
  invoiceQuantity: '',
  invoiceUnit: '',
  invoiceUnitPrice: '',
  lineTotal: null,
  inventoryItemId: '',
  quantity: '',
  unit: '',
  unitCost: '',
  currentUnitCost: 0,
  updateUnitCost: false,
  unitConverted: false,
  unitMismatch: false,
  matchStatus: 'UNMATCHED',
  confidence: 'HIGH',
  conversionNote: '',
  note: '',
  selected: true,
});

export const mapOcrResponseToReviewRows = (response, inventoryItems = []) => {
  const byId = new Map((inventoryItems || []).map((item) => [Number(item.id), item]));
  const lines = Array.isArray(response?.lines) ? response.lines : [];

  return lines.map((line) => {
    const matchedId = line.matchedInventoryItemId != null ? Number(line.matchedInventoryItemId) : null;
    const catalogItem = matchedId != null ? byId.get(matchedId) : null;
    const quantityValue =
      line.convertedQuantity != null ? line.convertedQuantity : line.invoiceQuantity;
    const unitCostValue =
      line.convertedUnitCost != null ? line.convertedUnitCost : line.invoiceUnitPrice;

    return {
      _key: nextRowKey(),
      rawName: line.rawName || '',
      invoiceQuantity: line.invoiceQuantity != null ? String(line.invoiceQuantity) : '',
      invoiceUnit: line.invoiceUnit || '',
      invoiceUnitPrice: line.invoiceUnitPrice != null ? String(line.invoiceUnitPrice) : '',
      lineTotal: line.lineTotal ?? null,
      inventoryItemId: matchedId != null ? String(matchedId) : '',
      quantity: quantityValue != null ? String(quantityValue) : '',
      unit: catalogItem?.unit || line.matchedInventoryUnit || line.invoiceUnit || '',
      unitCost: unitCostValue != null ? String(unitCostValue) : '',
      currentUnitCost: Number(catalogItem?.unitCost ?? line.currentUnitCost ?? 0),
      updateUnitCost: false,
      unitConverted: Boolean(line.unitConverted),
      unitMismatch: Boolean(line.unitMismatch),
      matchStatus:
        line.matchStatus ||
        (matchedId != null ? (line.unitMismatch ? 'UNIT_MISMATCH' : 'MATCHED') : 'UNMATCHED'),
      confidence: line.confidence || 'MEDIUM',
      conversionNote: line.conversionNote || '',
      note: '',
      selected: matchedId != null,
    };
  });
};

export const recalculateRowForTargetItem = (
  row,
  targetItemId,
  inventoryItems = [],
  language = 'vi',
) => {
  const isEn = language === 'en';
  const targetItem = (inventoryItems || []).find(
    (item) => String(item.id) === String(targetItemId),
  );

  if (!targetItem) {
    return {
      ...row,
      inventoryItemId: '',
      unit: row.invoiceUnit || '',
      currentUnitCost: 0,
      unitConverted: false,
      unitMismatch: false,
      matchStatus: 'UNMATCHED',
      conversionNote: isEn
        ? 'Please select an inventory item before importing.'
        : 'Vui lòng chọn nguyên liệu trước khi nhập.',
    };
  }

  const baseQty = Number(row.invoiceQuantity);
  const hasInvoiceBasis = Number.isFinite(baseQty) && baseQty > 0;

  if (!hasInvoiceBasis) {
    return {
      ...row,
      inventoryItemId: String(targetItem.id),
      unit: targetItem.unit || '',
      currentUnitCost: Number(targetItem.unitCost ?? 0),
      unitConverted: false,
      unitMismatch: false,
      matchStatus: 'MATCHED',
      conversionNote: '',
      selected: true,
    };
  }

  const rawPrice =
    row.invoiceUnitPrice !== '' && row.invoiceUnitPrice != null
      ? Number(row.invoiceUnitPrice)
      : null;
  const conversion = convertInvoiceUnit(
    baseQty,
    rawPrice,
    row.invoiceUnit,
    targetItem.unit,
    language,
  );

  return {
    ...row,
    inventoryItemId: String(targetItem.id),
    quantity: String(conversion.convertedQuantity),
    unit: targetItem.unit || '',
    unitCost:
      conversion.convertedUnitCost != null ? String(conversion.convertedUnitCost) : row.unitCost,
    currentUnitCost: Number(targetItem.unitCost ?? 0),
    unitConverted: conversion.unitConverted,
    unitMismatch: conversion.unitMismatch,
    matchStatus: conversion.unitMismatch ? 'UNIT_MISMATCH' : 'MATCHED',
    conversionNote: conversion.note,
    selected: true,
  };
};

export const buildBatchStockInPayload = (header = {}, rows = [], isManager = false, language = 'vi') => {
  const isEn = language === 'en';
  const selectedRows = (rows || []).filter((row) => row.selected);

  if (selectedRows.length === 0) {
    return {
      valid: false,
      error: isEn
        ? 'Select at least one invoice line to stock in.'
        : 'Hãy chọn ít nhất 1 dòng nguyên liệu để nhập kho.',
      payload: null,
    };
  }

  if (selectedRows.length > MAX_BATCH_STOCK_IN_LINES) {
    return {
      valid: false,
      error: isEn
        ? `Cannot import more than ${MAX_BATCH_STOCK_IN_LINES} lines at once.`
        : `Không thể nhập quá ${MAX_BATCH_STOCK_IN_LINES} dòng trong một lần.`,
      payload: null,
    };
  }

  const items = [];
  for (let index = 0; index < selectedRows.length; index += 1) {
    const row = selectedRows[index];
    const itemId = Number(row.inventoryItemId);
    if (!Number.isInteger(itemId) || itemId <= 0) {
      return {
        valid: false,
        error: isEn
          ? `Line "${row.rawName || index + 1}" is not matched to an inventory item.`
          : `Dòng "${row.rawName || index + 1}" chưa được ghép với nguyên liệu trong kho.`,
        payload: null,
      };
    }

    const quantity = Number(row.quantity);
    if (!Number.isFinite(quantity) || quantity <= 0) {
      return {
        valid: false,
        error: isEn
          ? `Quantity for line "${row.rawName || index + 1}" must be greater than 0.`
          : `Số lượng dòng "${row.rawName || index + 1}" phải lớn hơn 0.`,
        payload: null,
      };
    }

    let parsedCost = null;
    if (isManager && row.unitCost !== '' && row.unitCost != null) {
      parsedCost = Number(row.unitCost);
      if (!Number.isFinite(parsedCost) || parsedCost < 0) {
        return {
          valid: false,
          error: isEn
            ? `Unit cost for line "${row.rawName || index + 1}" cannot be negative.`
            : `Giá vốn dòng "${row.rawName || index + 1}" không được âm.`,
          payload: null,
        };
      }
      parsedCost = roundTo(parsedCost, 2);
    }

    const canUpdateCost = Boolean(isManager && row.updateUnitCost && parsedCost !== null);

    items.push({
      inventoryItemId: itemId,
      quantity: roundTo(quantity, 3),
      unitCost: parsedCost,
      updateUnitCost: canUpdateCost,
      note: row.note && row.note.trim() ? row.note.trim().slice(0, 300) : null,
    });
  }

  return {
    valid: true,
    error: '',
    payload: {
      supplierName: header.supplierName?.trim() ? header.supplierName.trim().slice(0, 150) : null,
      invoiceRef: header.invoiceRef?.trim() ? header.invoiceRef.trim().slice(0, 100) : null,
      note: header.note?.trim() ? header.note.trim().slice(0, 300) : null,
      items,
    },
  };
};
