import { createElement } from 'react';
import { Boxes, PackagePlus, Pencil, SlidersHorizontal } from 'lucide-react';

import { fmtQty, numberValue } from '@features/inventory-management/lib/inventoryFormat.js';
import StockBadge from './StockBadge.jsx';

const ActionButton = ({ icon, label, onClick, tone }) => {
  const classes =
    tone === 'orange'
      ? 'border border-orange-100 bg-orange-50 text-orange-600 hover:bg-orange-600 hover:text-white dark:border-orange-500/20 dark:bg-orange-500/10 dark:text-orange-300'
      : 'border border-slate-200 bg-slate-50 text-slate-600 hover:bg-slate-800 hover:text-white dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300';
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex-1 flex h-11 items-center justify-center gap-2 rounded-xl text-sm font-bold transition-all ${classes}`}
    >
      {createElement(icon, { size: 16 })} {label}
    </button>
  );
};

const InventoryCard = ({ item, onEdit, onStockIn, onAdjust }) => {
  const ratio =
    item.lowStockThreshold > 0
      ? Math.min(
          100,
          Math.round(
            (numberValue(item.quantityOnHand) / numberValue(item.lowStockThreshold)) * 100,
          ),
        )
      : 100;

  return (
    <div className="group relative flex h-full flex-col overflow-hidden rounded-3xl border border-slate-100 bg-white p-6 shadow-sm transition-all hover:border-orange-500 hover:shadow-xl dark:border-slate-800 dark:bg-slate-900">
      {/* Trang trí góc thẻ */}
      <div className="absolute -right-6 -top-6 h-20 w-20 rounded-full bg-orange-50 transition-colors group-hover:bg-orange-100 dark:bg-orange-500/10 dark:group-hover:bg-orange-500/20" />

      <div className="mb-5 flex items-start justify-between gap-3 relative z-10">
        <div className="flex min-w-0 items-center gap-3">
          <div className="shrink-0 rounded-2xl bg-orange-50 p-3 text-orange-500 dark:bg-orange-500/10 dark:text-orange-300">
            <Boxes size={22} />
          </div>
          <div className="min-w-0">
            <h3 className="truncate text-lg font-bold text-slate-800 transition-colors group-hover:text-orange-600 dark:text-slate-100 dark:group-hover:text-orange-300">
              {item.name}
            </h3>
            <p className="text-xs font-bold text-slate-400 dark:text-slate-500">{item.unit}</p>
          </div>
        </div>
        <StockBadge item={item} />
      </div>

      <div className="mb-4 relative z-10">
        <p className="text-[10px] font-black uppercase tracking-[0.18em] text-slate-400 dark:text-slate-500">
          Tồn hiện tại
        </p>
        <div className="mt-1 flex items-end gap-2">
          <span className="text-3xl font-black tracking-tight text-slate-900 dark:text-slate-100">
            {fmtQty(item.quantityOnHand)}
          </span>
          <span className="pb-1 text-sm font-bold text-slate-400 dark:text-slate-500">{item.unit}</span>
        </div>
      </div>

      <div className="mb-6 flex-grow relative z-10">
        <div className="mb-2 flex items-center justify-between text-xs font-bold">
          <span className="text-slate-400 dark:text-slate-500">Ngưỡng cảnh báo</span>
          <span className="text-slate-700 dark:text-slate-300">
            {fmtQty(item.lowStockThreshold)} {item.unit}
          </span>
        </div>
        <div className="h-2 rounded-full bg-slate-100 dark:bg-slate-800">
          <div
            className={`h-2 rounded-full ${item.outOfStock ? 'bg-red-500' : item.lowStock ? 'bg-amber-400' : 'bg-emerald-500'}`}
            style={{ width: `${item.outOfStock ? 4 : Math.max(8, ratio)}%` }}
          />
        </div>
      </div>

      <div className="flex gap-2 mt-auto relative z-10">
        <ActionButton
          icon={PackagePlus}
          label="Nhập"
          onClick={() => onStockIn(item)}
          tone="orange"
        />
        <ActionButton
          icon={SlidersHorizontal}
          label="Kiểm kê"
          onClick={() => onAdjust(item)}
          tone="slate"
        />
        <button
          type="button"
          title="Sửa"
          onClick={() => onEdit(item)}
          className="flex h-11 flex-[0.3] items-center justify-center rounded-xl border border-blue-100 bg-blue-50 text-blue-600 transition-colors hover:bg-blue-600 hover:text-white dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-300"
        >
          <Pencil size={18} />
        </button>
      </div>
    </div>
  );
};

export default InventoryCard;
