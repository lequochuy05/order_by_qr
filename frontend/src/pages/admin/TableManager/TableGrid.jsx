import React from 'react';
import TableCard from './TableCard'; 

const TableGrid = ({ tables, orders, onAction, userRole }) => {
    if (tables.length === 0) {
        return (
            <div className="text-center py-20 bg-white rounded-3xl border border-dashed">
                <p className="text-gray-400 italic">Chưa có bàn nào hoặc không tìm thấy bàn phù hợp.</p>
            </div>
        );
    }

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {tables.map(table => (
                <TableCard 
                    key={table.id}
                    table={table}
                    order={orders[table.id]}
                    onDetail={() => onAction({ type: 'DETAIL', table, order: orders[table.id] })}
                    onAddItems={() => onAction({ type: 'ADD_ITEM', table })}
                    onPay={() => onAction({ type: 'PAY', table, order: orders[table.id] })}
                    
                    onEdit={() => onAction({ type: 'EDIT', table })}
                    onDelete={() => onAction({ type: 'DELETE', table })}
                    
                    userRole={userRole} // Truyền xuống card
                />
            ))}
        </div>
    );
};

export default TableGrid;