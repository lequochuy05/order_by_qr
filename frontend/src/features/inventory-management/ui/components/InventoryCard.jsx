import { createElement } from 'react';
import { Boxes, PackagePlus, Pencil, SlidersHorizontal } from 'lucide-react';

import { fmtQty, numberValue } from '@features/inventory-management/lib/inventoryFormat.js';
import StockBadge from './StockBadge.jsx';

const ActionButton = ({ icon, label, onClick, tone }) => {
  const classes =
    tone === 'orange'
      ? 'border border-[#C17C4A]/30 bg-[#F7F0E7] text-[#56372B] hover:bg-[#56372B] hover:text-white dark:border-[#C17C4A]/30 dark:bg-[#2A221E] dark:text-[#D8986A]'
      : 'border border-[#E8DCD1] bg-white text-[#6B5D55] hover:bg-[#F7F0E7] dark:border-[#352C26] dark:bg-[#1F1A17] dark:text-[#BDB0A4]';
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
    <div className="group relative flex h-full flex-col overflow-hidden rounded-3xl border border-[#E8DCD1] bg-white p-6 shadow-sm transition-all hover:border-[#C17C4A] hover:shadow-xl dark:border-[#352C26] dark:bg-[#1F1A17]">
      {/* Trang trí góc thẻ */}
      <div className="absolute -right-6 -top-6 h-20 w-20 rounded-full bg-[#F7F0E7] transition-colors group-hover:bg-[#EAE0D5] dark:bg-[#2A221E] dark:group-hover:bg-[#352C26]" />

      <div className="mb-5 flex items-start justify-between gap-3 relative z-10">
        <div className="flex min-w-0 items-center gap-3">
          <div className="shrink-0 rounded-2xl bg-[#F7F0E7] p-3 text-[#C17C4A] dark:bg-[#2A221E] dark:text-[#D8986A]">
            <Boxes size={22} />
          </div>
          <div className="min-w-0">
            <h3 className="truncate text-lg font-bold text-[#2D1D16] transition-colors group-hover:text-[#C17C4A] dark:text-[#F7F0E7] dark:group-hover:text-[#D8986A]">
              {item.name}
            </h3>
            <p className="text-xs font-bold text-[#9C8E85] dark:text-[#82756B]">{item.unit}</p>
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
        {isManager && (
          <button
            type="button"
            title="Sửa"
            onClick={() => onEdit(item)}
            className="flex h-11 flex-[0.3] items-center justify-center rounded-xl border border-blue-100 bg-blue-50 text-blue-600 transition-colors hover:bg-blue-600 hover:text-white dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-300"
          >
            <Pencil size={18} />
          </button>
        )}
      </div>
    </div>
  );
};

export default InventoryCard;
