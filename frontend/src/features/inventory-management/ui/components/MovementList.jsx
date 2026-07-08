import { ClipboardList, History } from 'lucide-react';

import { movementLabels } from '@features/inventory-management/lib/inventoryConstants.js';
import { fmtQty } from '@features/inventory-management/lib/inventoryFormat.js';

const MovementList = ({ movements }) => (
  <div className="rounded-3xl border border-slate-100 bg-white shadow-sm transition-colors dark:border-slate-800 dark:bg-slate-900">
    <div className="flex items-center gap-2 border-b border-slate-100 px-5 py-4 font-black text-slate-800 dark:border-slate-800 dark:text-slate-100">
      <History size={18} /> Lịch sử kho gần đây
    </div>
    <div className="divide-y divide-slate-100 dark:divide-slate-800">
      {movements.map((movement) => (
        <div
          key={movement.id}
          className="flex flex-wrap items-center justify-between gap-4 px-5 py-4 text-sm"
        >
          <div className="flex min-w-0 items-center gap-3">
            <div className="rounded-2xl bg-slate-50 p-3 text-slate-500 dark:bg-slate-950 dark:text-slate-400">
              <ClipboardList size={18} />
            </div>
            <div className="min-w-0">
              <p className="truncate font-black text-slate-800 dark:text-slate-100">{movement.inventoryItemName}</p>
              <p className="text-xs font-bold text-slate-400 dark:text-slate-500">
                {movementLabels[movement.movementType] || movement.movementType}
              </p>
            </div>
          </div>
          <div className="text-right">
            <p className="font-black text-slate-800 dark:text-slate-100">
              {fmtQty(movement.quantity)} {movement.unit}
            </p>
            <p className="text-xs font-bold text-slate-400 dark:text-slate-500">
              {fmtQty(movement.quantityBefore)} → {fmtQty(movement.quantityAfter)}
            </p>
          </div>
        </div>
      ))}
      {movements.length === 0 && (
        <div className="py-20 text-center italic text-slate-400 dark:text-slate-500">
          <History size={40} className="mx-auto mb-4 opacity-30" />
          Chưa có lịch sử kho.
        </div>
      )}
    </div>
  </div>
);

export default MovementList;
