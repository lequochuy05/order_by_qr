import React from 'react';
import { LayoutGrid } from 'lucide-react';
import EmptyState from '@shared/ui/EmptyState.jsx';
import TableCard from './TableCard';

const TableGrid = ({ tables, orders = [], onAction, userRole }) => {
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

        return (
          <TableCard
            key={table.id}
            table={table}
            order={tableOrder}
            onDetail={() => onAction({ type: 'DETAIL', table, order: tableOrder })}
            onAddItems={() => onAction({ type: 'ADD_ITEM', table })}
            onPay={() => onAction({ type: 'PAY', table, order: tableOrder })}
            onEdit={() => onAction({ type: 'EDIT', table })}
            onDelete={() => onAction({ type: 'DELETE', table })}
            userRole={userRole}
          />
        );
      })}
    </div>
  );
};

export default TableGrid;
