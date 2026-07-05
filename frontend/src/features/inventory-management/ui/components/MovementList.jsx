import { ClipboardList, History } from 'lucide-react';

import { movementLabels } from '@features/inventory-management/lib/inventoryConstants.js';
import { fmtQty } from '@features/inventory-management/lib/inventoryFormat.js';

const MovementList = ({ movements }) => (
  <div className="rounded-3xl border border-gray-100 bg-white shadow-sm">
    <div className="flex items-center gap-2 border-b border-gray-100 px-5 py-4 font-black text-gray-800">
      <History size={18} /> Lịch sử kho gần đây
    </div>
    <div className="divide-y divide-gray-100">
      {movements.map((movement) => (
        <div
          key={movement.id}
          className="flex flex-wrap items-center justify-between gap-4 px-5 py-4 text-sm"
        >
          <div className="flex min-w-0 items-center gap-3">
            <div className="rounded-2xl bg-gray-50 p-3 text-gray-500">
              <ClipboardList size={18} />
            </div>
            <div className="min-w-0">
              <p className="truncate font-black text-gray-800">{movement.inventoryItemName}</p>
              <p className="text-xs font-bold text-gray-400">
                {movementLabels[movement.movementType] || movement.movementType}
              </p>
            </div>
          </div>
          <div className="text-right">
            <p className="font-black text-gray-800">
              {fmtQty(movement.quantity)} {movement.unit}
            </p>
            <p className="text-xs font-bold text-gray-400">
              {fmtQty(movement.quantityBefore)} → {fmtQty(movement.quantityAfter)}
            </p>
          </div>
        </div>
      ))}
      {movements.length === 0 && (
        <div className="py-20 text-center italic text-gray-400">
          <History size={40} className="mx-auto mb-4 opacity-30" />
          Chưa có lịch sử kho.
        </div>
      )}
    </div>
  </div>
);

export default MovementList;
