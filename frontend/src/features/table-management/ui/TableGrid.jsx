import React from 'react';
import TableCard from './TableCard';

const TableGrid = ({ tables, orders = [], onAction, userRole }) => {
  if (tables.length === 0) {
    return (
      <div className="text-center py-20 bg-white rounded-3xl border border-dashed">
        <p className="text-gray-400 italic">Chưa có bàn nào hoặc không tìm thấy bàn phù hợp.</p>
      </div>
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
