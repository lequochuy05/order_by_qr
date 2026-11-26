import { ORDER_ITEM_STATUSES } from './orderConstants.js';

export const getOrderSubtotalAmount = (order) => order?.subtotalAmount ?? 0;

export const getOrderDiscountAmount = (order) => order?.discountAmount ?? 0;

export const getOrderServiceChargePercent = (order) =>
  Number(order?.serviceChargePercent ?? 0) || 0;

export const getOrderServiceChargeAmount = (order) =>
  Number(order?.serviceChargeAmount ?? 0) || 0;

export const getOrderTaxPercent = (order) => Number(order?.taxPercent ?? 0) || 0;

export const getOrderTaxAmount = (order) => Number(order?.taxAmount ?? 0) || 0;

export const getOrderFinalAmount = (order) => order?.finalAmount ?? 0;

export const getOrderPaidAmount = (order) => order?.paidAmount ?? getOrderFinalAmount(order);

export const calculateTaxAndServiceCharge = (subtotalAmount, discountAmount, settings = {}) => {
  const safeSubtotal = Math.max(0, Number(subtotalAmount || 0));
  const safeDiscount = Math.min(safeSubtotal, Math.max(0, Number(discountAmount || 0)));
  const netSubtotal = Math.max(0, safeSubtotal - safeDiscount);

  const serviceChargePercent = Math.max(0, Number(settings?.serviceChargePercent || 0));
  const taxPercent = Math.max(0, Number(settings?.taxPercent || 0));

  const serviceChargeAmount =
    serviceChargePercent > 0 ? Math.round((netSubtotal * serviceChargePercent) / 100) : 0;
  const taxableBase = netSubtotal + serviceChargeAmount;
  const taxAmount = taxPercent > 0 ? Math.round((taxableBase * taxPercent) / 100) : 0;
  const finalAmount = taxableBase + taxAmount;

  return {
    subtotalAmount: safeSubtotal,
    discountAmount: safeDiscount,
    serviceChargePercent,
    serviceChargeAmount,
    taxPercent,
    taxAmount,
    finalAmount,
  };
};

export const isBillableOrderItem = (item) =>
  Boolean(item) && item.status !== ORDER_ITEM_STATUSES.CANCELLED;

export const getBillableOrderItems = (order) => (order?.orderItems || []).filter(isBillableOrderItem);
