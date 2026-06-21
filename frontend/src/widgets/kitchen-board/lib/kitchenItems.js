import { CheckCircle2, Clock3, Flame } from 'lucide-react';

export const OVERDUE_MINUTES = 20;

export const KITCHEN_COLUMNS = [
  {
    title: 'Chờ nấu',
    subtitle: 'Ưu tiên món chờ lâu nhất',
    status: 'PENDING',
    icon: Clock3,
    tone: 'orange',
  },
  {
    title: 'Đang nấu',
    subtitle: 'Các món đang được chế biến',
    status: 'COOKING',
    icon: Flame,
    tone: 'blue',
  },
  {
    title: 'Hoàn thành',
    subtitle: 'Chờ phục vụ hoặc thanh toán',
    status: 'FINISHED',
    icon: CheckCircle2,
    tone: 'green',
  },
];

const minutesSince = (dateValue, now) => {
  const timestamp = new Date(dateValue).getTime();
  if (Number.isNaN(timestamp)) return 0;
  return Math.max(0, Math.floor((now - timestamp) / 60_000));
};

export const buildKitchenItems = (orders, now) =>
  orders.flatMap((order) =>
    (order.orderItems || [])
      .filter((item) => ['PENDING', 'COOKING', 'FINISHED'].includes(item.status))
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
          isOverdue: item.status !== 'FINISHED' && waitMinutes >= OVERDUE_MINUTES,
          hasNotes: Boolean(item.notes?.trim()),
        };
      }),
  );

export const groupKitchenItems = (items) => {
  const grouped = { PENDING: [], COOKING: [], FINISHED: [] };
  items.forEach((item) => grouped[item.status].push(item));
  grouped.PENDING.sort((a, b) => b.waitMinutes - a.waitMinutes);
  grouped.COOKING.sort((a, b) => b.waitMinutes - a.waitMinutes);
  grouped.FINISHED.sort((a, b) => new Date(b.statusUpdatedAt) - new Date(a.statusUpdatedAt));
  return grouped;
};

export const summarizeKitchenItems = (items) => {
  const activeItems = items.filter((item) => item.status !== 'FINISHED');
  const totalWait = activeItems.reduce((sum, item) => sum + item.waitMinutes, 0);

  return {
    pending: items.filter((item) => item.status === 'PENDING').length,
    cooking: items.filter((item) => item.status === 'COOKING').length,
    finished: items.filter((item) => item.status === 'FINISHED').length,
    overdue: activeItems.filter((item) => item.isOverdue).length,
    averageWait: activeItems.length ? Math.round(totalWait / activeItems.length) : 0,
  };
};
