import { CheckCircle2, Clock3, Flame } from 'lucide-react';

import { ORDER_ITEM_STATUSES } from '@entities/order/lib/orderConstants.js';

export const DEFAULT_OVERDUE_MINUTES = 20;
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

        return {
          ...item,
          itemName,
          category: item.menuItem?.category?.name || (item.combo ? 'Combo' : 'Khác'),
          tableName: order.table?.tableNumber || 'Chưa gán',
          orderId: order.id,
          orderCreatedAt: order.createdAt,
          createdAt,
          statusUpdatedAt,
          waitMinutes,
          stageMinutes: minutesSince(statusUpdatedAt, now),
          isOverdue: item.status !== ORDER_ITEM_STATUSES.FINISHED && waitMinutes >= overdueMinutes,
          hasNotes: Boolean(item.notes?.trim()),
        };
      }),
  );

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
    averageWait: activeItems.length ? Math.round(totalWait / activeItems.length) : 0,
  };
};
