import { Boxes } from 'lucide-react';

import InventoryCard from './InventoryCard.jsx';

const InventoryGrid = ({ items, onEdit, onStockIn, onAdjust, isManager = true }) => {
  if (items.length === 0) {
    return (
      <div className="rounded-3xl border border-dashed border-slate-200 bg-white py-20 text-center italic text-slate-400 transition-colors dark:border-slate-800 dark:bg-slate-900 dark:text-slate-500">
        <Boxes size={40} className="mx-auto mb-4 opacity-30" />
        Không tìm thấy nguyên liệu phù hợp hoặc chưa có dữ liệu.
      </div>
    );
  }

  return (
    <div className="grid min-w-0 grid-cols-1 gap-4 sm:gap-5 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
      {items.map((item) => (
        <InventoryCard
          key={item.id}
          item={item}
          onEdit={onEdit}
          onStockIn={onStockIn}
          onAdjust={onAdjust}
          isManager={isManager}
        />
      ))}
    </div>
  );
};

export default InventoryGrid;
