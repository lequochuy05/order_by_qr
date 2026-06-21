export const INVENTORY_UNITS = ['g', 'kg', 'ml', 'l'];
export const INVENTORY_PAGE_SIZE = 24;

export const STOCK_FILTER_OPTIONS = [
  { id: 'LOW', name: 'Sắp hết' },
  { id: 'OUT', name: 'Hết hàng' },
  { id: 'INACTIVE', name: 'Tạm ngưng' },
];

export const movementLabels = {
  STOCK_IN: 'Nhập kho',
  ADJUSTMENT: 'Kiểm kê',
  ORDER_RESERVE: 'Trừ theo đơn',
  ORDER_RELEASE: 'Hoàn theo đơn',
};

export const defaultInventoryForm = () => ({
  name: '',
  unit: INVENTORY_UNITS[0],
  quantityOnHand: 0,
  lowStockThreshold: 0,
  active: true,
});
