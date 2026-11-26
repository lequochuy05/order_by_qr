import { CheckCircle2, Clock3, Flame } from 'lucide-react';

// eslint-disable-next-line no-restricted-imports
import { ORDER_ITEM_STATUSES } from '../../../entities/order/lib/orderConstants.js';
// eslint-disable-next-line no-restricted-imports
import {
  analyzeCustomerNote,
  RISK_LEVELS,
} from '../../../entities/ai/lib/noteSafety.js';

const DEFAULT_OVERDUE_MINUTES = 20;
export const OVERDUE_MINUTES = DEFAULT_OVERDUE_MINUTES;

const VISIBLE_KITCHEN_STATUSES = [
  ORDER_ITEM_STATUSES.PENDING,
  ORDER_ITEM_STATUSES.COOKING,
  ORDER_ITEM_STATUSES.FINISHED,
];

export const KITCHEN_COLUMNS = [
  {
    title: 'Chờ nấu',
    subtitle: 'Ưu tiên món chờ lâu nhất',
    status: ORDER_ITEM_STATUSES.PENDING,
    icon: Clock3,
    tone: 'orange',
  },
  {
    title: 'Đang nấu',
    subtitle: 'Các món đang được chế biến',
    status: ORDER_ITEM_STATUSES.COOKING,
    icon: Flame,
    tone: 'blue',
  },
  {
    title: 'Hoàn thành',
    subtitle: 'Chờ phục vụ hoặc thanh toán',
    status: ORDER_ITEM_STATUSES.FINISHED,
    icon: CheckCircle2,
    tone: 'green',
  },
];

const minutesSince = (dateValue, now) => {
  const timestamp = new Date(dateValue).getTime();
  if (Number.isNaN(timestamp)) return 0;
  return Math.max(0, Math.floor((now - timestamp) / 60_000));
};

export const buildKitchenItems = (orders, now, overdueMinutes = DEFAULT_OVERDUE_MINUTES) =>
  orders.flatMap((order) =>
    (order.orderItems || [])
      .filter((item) => VISIBLE_KITCHEN_STATUSES.includes(item.status))
      .map((item) => {
        const createdAt = item.createdAt || order.createdAt;
        const statusUpdatedAt = item.updatedAt || createdAt;
        const waitMinutes = minutesSince(createdAt, now);
        const itemName =
          item.itemNameSnapshot || item.menuItem?.name || item.combo?.name || 'Món chưa xác định';
        const noteSafety = analyzeCustomerNote(item.notes);

        return {
          ...item,
          itemName,
          category: item.menuItem?.category?.name || (item.combo ? 'Combo' : 'Khác'),
          menuItemId: item.menuItem?.id ?? null,
          comboId: item.combo?.id ?? null,
          isCombo: Boolean(item.combo),
          isAvailable: item.combo
            ? item.combo.available !== false
            : item.menuItem
              ? item.menuItem.available !== false
              : true,
          tableName:
            order.table?.tableNumber || (order.orderType === 'TAKEAWAY' ? 'Mang về' : 'Chưa gán'),
          orderType: order.orderType || 'DINE_IN',
          orderPaymentStatus: order.paymentStatus || null,
          orderId: order.id,
          orderCreatedAt: order.createdAt,
          createdAt,
          statusUpdatedAt,
          waitMinutes,
          stageMinutes: minutesSince(statusUpdatedAt, now),
          isOverdue: item.status !== ORDER_ITEM_STATUSES.FINISHED && waitMinutes >= overdueMinutes,
          hasNotes: Boolean(item.notes?.trim()),
          noteSafety,
          hasAllergenAlert: noteSafety.riskLevel === RISK_LEVELS.CRITICAL,
          hasPriorityNote:
            noteSafety.riskLevel === RISK_LEVELS.CRITICAL ||
            noteSafety.riskLevel === RISK_LEVELS.HIGH,
          batchId: item.batchId,
          batchNumber: item.batchNumber || 1,
        };
      }),
  );

export const aggregateBatchCookingItems = (items) => {
  const activeItems = items.filter(
    (item) =>
      item.status === ORDER_ITEM_STATUSES.PENDING ||
      item.status === ORDER_ITEM_STATUSES.COOKING,
  );

  const groupsMap = new Map();

  for (const item of activeItems) {
    const key = `${item.itemName}__${item.category}`;
    if (!groupsMap.has(key)) {
      groupsMap.set(key, {
        key,
        itemName: item.itemName,
        category: item.category,
        menuItemId: item.menuItemId ?? null,
        comboId: item.comboId ?? null,
        isCombo: Boolean(item.isCombo),
        isAvailable: item.isAvailable !== false,
        totalQuantity: 0,
        pendingQuantity: 0,
        cookingQuantity: 0,
        items: [],
        pendingItems: [],
        cookingItems: [],
        maxWaitMinutes: 0,
        isOverdue: false,
      });
    }

    const group = groupsMap.get(key);
    group.totalQuantity += item.quantity || 1;
    group.items.push(item);
    if (!group.menuItemId && item.menuItemId) group.menuItemId = item.menuItemId;
    if (!group.comboId && item.comboId) group.comboId = item.comboId;
    group.isAvailable = group.isAvailable && item.isAvailable !== false;

    if (item.status === ORDER_ITEM_STATUSES.PENDING) {
      group.pendingQuantity += item.quantity || 1;
      group.pendingItems.push(item);
    } else if (item.status === ORDER_ITEM_STATUSES.COOKING) {
      group.cookingQuantity += item.quantity || 1;
      group.cookingItems.push(item);
    }

    if (item.waitMinutes > group.maxWaitMinutes) {
      group.maxWaitMinutes = item.waitMinutes;
    }
    if (item.isOverdue) {
      group.isOverdue = true;
    }
  }

  return Array.from(groupsMap.values()).sort((a, b) => b.maxWaitMinutes - a.maxWaitMinutes);
};

export const scaleRecipeItems = (recipeItems = [], multiplier = 1) => {
  const safeMultiplier = Math.max(1, Number(multiplier) || 1);
  return (Array.isArray(recipeItems) ? recipeItems : []).map((item) => {
    const quantityRequired = Number(item.quantityRequired || 0);
    const scaledQuantity = Number((quantityRequired * safeMultiplier).toFixed(3));
    const hasStockInfo = item.availableQuantity != null;
    const availableQuantity = hasStockInfo ? Number(item.availableQuantity) : null;
    const isOutOfStock = Boolean(item.outOfStock) || (hasStockInfo && availableQuantity <= 0);
    const isInsufficientStock = hasStockInfo && availableQuantity < scaledQuantity;
    const isLowStock = Boolean(item.lowStock);

    return {
      ...item,
      quantityRequired,
      scaledQuantity,
      availableQuantity,
      hasStockInfo,
      isInsufficientStock,
      isOutOfStock,
      isLowStock,
    };
  });
};

export const groupKitchenItems = (items) => {
  const grouped = {
    [ORDER_ITEM_STATUSES.PENDING]: [],
    [ORDER_ITEM_STATUSES.COOKING]: [],
    [ORDER_ITEM_STATUSES.FINISHED]: [],
  };
  items.forEach((item) => grouped[item.status].push(item));
  grouped[ORDER_ITEM_STATUSES.PENDING].sort((a, b) => b.waitMinutes - a.waitMinutes);
  grouped[ORDER_ITEM_STATUSES.COOKING].sort((a, b) => b.waitMinutes - a.waitMinutes);
  grouped[ORDER_ITEM_STATUSES.FINISHED].sort(
    (a, b) => new Date(b.statusUpdatedAt) - new Date(a.statusUpdatedAt),
  );
  return grouped;
};

export const summarizeKitchenItems = (items) => {
  const activeItems = items.filter((item) => item.status !== ORDER_ITEM_STATUSES.FINISHED);
  const totalWait = activeItems.reduce((sum, item) => sum + item.waitMinutes, 0);

  return {
    pending: items.filter((item) => item.status === ORDER_ITEM_STATUSES.PENDING).length,
    cooking: items.filter((item) => item.status === ORDER_ITEM_STATUSES.COOKING).length,
    finished: items.filter((item) => item.status === ORDER_ITEM_STATUSES.FINISHED).length,
    overdue: activeItems.filter((item) => item.isOverdue).length,
    allergenCount: activeItems.filter((item) => item.hasAllergenAlert).length,
    averageWait: activeItems.length ? Math.round(totalWait / activeItems.length) : 0,
  };
};
