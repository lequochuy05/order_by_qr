import { INVENTORY_UNITS } from './inventoryConstants.js';

export const numberValue = (value) => Number(value || 0);

export const fmtQty = (value) => {
  const number = numberValue(value);
  return Number.isInteger(number)
    ? number.toLocaleString('vi-VN')
    : number.toLocaleString('vi-VN', { maximumFractionDigits: 3 });
};

export const getInventoryUnitOptions = (currentUnit) => {
  const normalizedUnit = currentUnit?.trim();
  if (normalizedUnit && !INVENTORY_UNITS.includes(normalizedUnit)) {
    return [...INVENTORY_UNITS, normalizedUnit];
  }
  return INVENTORY_UNITS;
};
