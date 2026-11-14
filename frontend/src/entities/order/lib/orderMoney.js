import { ORDER_ITEM_STATUSES } from './orderConstants.js';

export const getOrderSubtotalAmount = (order) => order?.subtotalAmount ?? 0;

export const getOrderDiscountAmount = (order) => order?.discountAmount ?? 0;

export const getOrderFinalAmount = (order) => order?.finalAmount ?? 0;

export const getOrderPaidAmount = (order) => order?.paidAmount ?? getOrderFinalAmount(order);

export const isBillableOrderItem = (item) =>
  Boolean(item) && item.status !== ORDER_ITEM_STATUSES.CANCELLED;

export const getBillableOrderItems = (order) => (order?.orderItems || []).filter(isBillableOrderItem);
