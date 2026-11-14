import { createElement } from 'react';
import { Boxes, PackagePlus, Pencil, SlidersHorizontal } from 'lucide-react';

import { fmtQty, numberValue } from '@features/inventory-management/lib/inventoryFormat.js';
import StockBadge from './StockBadge.jsx';

const ActionButton = ({ icon, label, onClick, tone }) => {
  const classes =
    tone === 'indigo'
      ? 'border border-indigo-200/60 bg-indigo-50 text-indigo-700 hover:bg-indigo-600 hover:text-white dark:border-indigo-800/40 dark:bg-indigo-950/40 dark:text-indigo-300 dark:hover:bg-indigo-600 dark:hover:text-white'
      : 'border border-slate-200/80 bg-slate-50 text-slate-700 hover:bg-slate-100 hover:text-slate-900 dark:border-slate-800 dark:bg-slate-800/60 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white';
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex-1 flex h-11 items-center justify-center gap-2 rounded-xl text-xs font-bold transition-all active:scale-95 ${classes}`}
    >
      {createElement(icon, { size: 16 })} {label}
    </button>
  );
};

const InventoryCard = ({ item, onEdit, onStockIn, onAdjust, isManager = true }) => {
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
    <div className="group relative flex h-full flex-col overflow-hidden rounded-3xl border border-[#1E293B] bg-white/90 p-6 shadow-sm backdrop-blur-sm transition-all duration-300 hover:-translate-y-1 hover:border-indigo-500 hover:shadow-xl dark:border-[#1E293B] dark:bg-[#0F172A]/90 dark:hover:border-indigo-500/40">
      <div className="mb-5 flex items-start justify-between gap-3 relative z-10">
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-lg shadow-indigo-500/25 transition-transform duration-300 group-hover:scale-110">
            <Boxes size={22} />
          </div>
          <div className="min-w-0">
            <h3 className="truncate text-base font-bold text-slate-900 transition-colors group-hover:text-indigo-600 dark:text-white dark:group-hover:text-indigo-400">
              {item.name}
            </h3>
            <p className="text-xs font-semibold text-slate-400 dark:text-slate-500">{item.unit}</p>
          </div>
        </div>
        <StockBadge item={item} />
      </div>

      <div className="mb-4 relative z-10">
        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
          Tồn hiện tại
        </p>
        <div className="mt-1 flex items-end gap-2">
          <span className="text-3xl font-black font-mono tracking-tight text-slate-900 dark:text-white">
            {fmtQty(item.quantityOnHand)}
          </span>
          <span className="pb-1 text-xs font-semibold text-slate-400 dark:text-slate-500">{item.unit}</span>
        </div>
      </div>

      <div className="mb-6 flex-grow relative z-10">
        <div className="mb-2 flex items-center justify-between text-xs font-semibold">
          <span className="text-slate-400 dark:text-slate-500">Ngưỡng cảnh báo</span>
          <span className="font-mono text-slate-700 dark:text-slate-300">
            {fmtQty(item.lowStockThreshold)} {item.unit}
          </span>
        </div>
        <div className="h-2 rounded-full bg-slate-100 dark:bg-slate-800">
          <div
            className={`h-2 rounded-full transition-all duration-500 ${
              item.outOfStock
                ? 'bg-rose-500'
                : item.lowStock
                  ? 'bg-amber-400'
                  : 'bg-emerald-500'
            }`}
            style={{ width: `${item.outOfStock ? 4 : Math.max(8, ratio)}%` }}
          />
        </div>
      </div>

      <div className="flex gap-2 mt-auto relative z-10">
        <ActionButton
          icon={PackagePlus}
          label="Nhập"
          onClick={() => onStockIn(item)}
          tone="indigo"
        />
        <ActionButton
          icon={SlidersHorizontal}
          label="Kiểm kê"
          onClick={() => onAdjust(item)}
          tone="slate"
        />
        {isManager && (
          <button
            type="button"
            title="Sửa"
            onClick={() => onEdit(item)}
            className="flex h-11 flex-[0.3] items-center justify-center rounded-xl border border-indigo-200/60 bg-indigo-50 text-indigo-600 transition-all hover:bg-indigo-600 hover:text-white active:scale-95 dark:border-indigo-800/40 dark:bg-indigo-950/40 dark:text-indigo-400 dark:hover:bg-indigo-600 dark:hover:text-white"
          >
            <Pencil size={17} />
          </button>
        )}
      </div>
    </div>
  );
};

export default InventoryCard;
