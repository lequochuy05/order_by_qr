import { createElement } from 'react';
import { Boxes, PackagePlus, Pencil, SlidersHorizontal } from 'lucide-react';

import { fmtQty, numberValue } from '../../lib/inventoryFormat.js';
import StockBadge from './StockBadge.jsx';

const ActionButton = ({ icon, label, onClick, tone }) => {
  const classes =
    tone === 'orange'
      ? 'bg-orange-50 text-orange-600 hover:bg-orange-600 hover:text-white border border-orange-100'
      : 'bg-slate-50 text-slate-600 hover:bg-slate-800 hover:text-white border border-slate-200';
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
    <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 group hover:border-orange-500 hover:shadow-xl transition-all flex flex-col h-full relative overflow-hidden">
      {/* Trang trí góc thẻ */}
      <div className="absolute -top-6 -right-6 w-20 h-20 bg-orange-50 rounded-full group-hover:bg-orange-100 transition-colors" />

      <div className="mb-5 flex items-start justify-between gap-3 relative z-10">
        <div className="flex min-w-0 items-center gap-3">
          <div className="shrink-0 rounded-2xl bg-orange-50 p-3 text-orange-500">
            <Boxes size={22} />
          </div>
          <div className="min-w-0">
            <h3 className="truncate text-lg font-bold text-gray-800 group-hover:text-orange-600 transition-colors">
              {item.name}
            </h3>
            <p className="text-xs font-bold text-gray-400">{item.unit}</p>
          </div>
        </div>
        <StockBadge item={item} />
      </div>

      <div className="mb-4 relative z-10">
        <p className="text-[10px] font-black uppercase tracking-[0.18em] text-gray-400">
          Tồn hiện tại
        </p>
        <div className="mt-1 flex items-end gap-2">
          <span className="text-3xl font-black tracking-tight text-gray-900">
            {fmtQty(item.quantityOnHand)}
          </span>
          <span className="pb-1 text-sm font-bold text-gray-400">{item.unit}</span>
        </div>
      </div>

      <div className="mb-6 flex-grow relative z-10">
        <div className="mb-2 flex items-center justify-between text-xs font-bold">
          <span className="text-gray-400">Ngưỡng cảnh báo</span>
          <span className="text-gray-700">
            {fmtQty(item.lowStockThreshold)} {item.unit}
          </span>
        </div>
        <div className="h-2 rounded-full bg-gray-100">
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
          className="flex flex-[0.3] h-11 items-center justify-center rounded-xl bg-blue-50 text-blue-600 transition-colors hover:bg-blue-600 hover:text-white border border-blue-100"
        >
          <Pencil size={18} />
        </button>
      </div>
    </div>
  );
};

export default InventoryCard;
