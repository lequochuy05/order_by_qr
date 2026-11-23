import React from 'react';
import { LayoutGrid } from 'lucide-react';
import EmptyState from '@shared/ui/EmptyState.jsx';
import TableCard from './TableCard';

const resolveTableServiceRequestSummary = (table, serviceRequestsByTable = {}) => {
  const ids = new Set(
    [table.id, ...(Array.isArray(table.memberTableIds) ? table.memberTableIds : [])]
      .filter((id) => id != null)
      .map(String),
  );

  let openCount = 0;
  let pendingCount = 0;
  let hasOverdue = false;

  ids.forEach((id) => {
    const entry = serviceRequestsByTable[id];
    if (!entry) return;
    openCount += entry.openCount || 0;
    pendingCount += entry.pendingCount || 0;
    if (entry.hasOverdue) hasOverdue = true;
  });

  return openCount > 0 ? { openCount, pendingCount, hasOverdue } : null;
};

const TableGrid = ({
  tables,
  orders = [],
  onAction,
  userRole,
  serviceRequestsByTable = {},
  onOpenServiceRequests,
}) => {
  if (tables.length === 0) {
    return (
      <EmptyState icon={LayoutGrid} message="Chưa có bàn nào hoặc không tìm thấy bàn phù hợp." />
    );
  }

  return (
    <div className="grid min-w-0 grid-cols-1 gap-4 sm:gap-6 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {tables.map((table) => {
        // Hỗ trợ cả trường hợp orders là Mảng hoặc Object
        const tableOrder = Array.isArray(orders)
          ? orders.find((o) => o.table?.id === table.id)
          : orders[table.id];
        const serviceRequestSummary = resolveTableServiceRequestSummary(
          table,
          serviceRequestsByTable,
        );

        return (
          <TableCard
            key={table.id}
            table={table}
            order={tableOrder}
            onDetail={() => onAction({ type: 'DETAIL', table, order: tableOrder })}
            onAddItems={() => onAction({ type: 'ADD_ITEM', table })}
            onPay={() => onAction({ type: 'PAY', table, order: tableOrder })}
            onMerge={() => onAction({ type: 'MERGE', table })}
            onEdit={() => onAction({ type: 'EDIT', table })}
            onDelete={() => onAction({ type: 'DELETE', table })}
            userRole={userRole}
            serviceRequestSummary={serviceRequestSummary}
            onOpenServiceRequests={
              onOpenServiceRequests ? () => onOpenServiceRequests(table.id) : undefined
            }
          />
        );
      })}
    </div>
  );
};

export default TableGrid;
