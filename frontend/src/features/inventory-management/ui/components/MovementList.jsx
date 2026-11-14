import { ClipboardList, History } from 'lucide-react';

import { movementLabels } from '@features/inventory-management/lib/inventoryConstants.js';
import { fmtQty } from '@features/inventory-management/lib/inventoryFormat.js';

const MovementList = ({ movements }) => (
  <div className="rounded-3xl border border-[#1E293B] bg-white/90 shadow-sm backdrop-blur-sm transition-colors dark:border-[#1E293B] dark:bg-[#0F172A]/90">
    <div className="flex items-center gap-2.5 border-b border-[#1E293B]/20 px-6 py-4 font-bold text-slate-900 dark:border-[#1E293B] dark:text-white">
      <History size={18} className="text-indigo-600 dark:text-indigo-400" /> Lịch sử kho gần đây
    </div>
    <div className="divide-y divide-slate-100 dark:divide-slate-800/60">
      {movements.map((movement) => (
        <div
          key={movement.id}
          className="flex flex-wrap items-center justify-between gap-4 px-6 py-4 text-sm hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors"
        >
          <div className="flex min-w-0 items-center gap-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-400">
              <ClipboardList size={18} />
            </div>
            <div className="min-w-0">
              <p className="truncate font-bold text-slate-900 dark:text-white">
                {movement.inventoryItemName}
              </p>
              <p className="text-xs font-semibold text-slate-400 dark:text-slate-500">
                {movementLabels[movement.movementType] || movement.movementType}
              </p>
            </div>
          </div>
          <div className="text-right">
            <p className="font-black font-mono text-slate-900 dark:text-white">
              {fmtQty(movement.quantity)} {movement.unit}
            </p>
            <p className="text-xs font-bold font-mono text-slate-400 dark:text-slate-500">
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
