import React from 'react';
import { ChevronRight, Clock3, ReceiptText } from 'lucide-react';

import { fmtVND } from '@shared/lib/formatters.js';
import { getOrderStatusMeta } from '@entities/order/lib/orderStatus.js';
import { getOrderFinalAmount } from '@entities/order/lib/orderMoney.js';

const countItems = (order) =>
  (order?.items || order?.orderItems || []).reduce(
    (sum, item) => sum + (Number(item.quantity) || 0),
    0,
  );

const CurrentOrderBanner = ({ order, onClick }) => {
  if (!order) return null;

  const status = getOrderStatusMeta(order.status);
  const itemCount = countItems(order);

  return (
    <button
      type="button"
      onClick={onClick}
      className="w-full rounded-2xl border border-slate-200/80 bg-white p-3 text-left shadow-sm transition-all active:scale-[0.99] dark:border-slate-800/80 dark:bg-slate-900/90"
    >
      <div className="flex items-center gap-2.5">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-400">
          <ReceiptText size={18} />
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <p className="truncate text-xs font-mono font-bold text-slate-900 dark:text-white">
              Đơn #{order.id}
            </p>
            <span
              className={`shrink-0 rounded-full border px-2 py-0.5 text-[10px] font-bold ${status.classes}`}
            >
              {status.label}
            </span>
          </div>
          <div className="mt-0.5 flex items-center gap-1.5 text-xs font-semibold text-slate-400 dark:text-slate-500">
            <Clock3 size={11} />
            <span className="truncate">{status.helper}</span>
            <span className="shrink-0">•</span>
            <span className="shrink-0">{itemCount} món</span>
          </div>
        </div>

        <div className="shrink-0 text-right">
          <p className="text-xs font-black font-mono text-indigo-600 dark:text-indigo-400">
            {fmtVND(getOrderFinalAmount(order))}
          </p>
          <ChevronRight className="ml-auto mt-0.5 text-slate-400 dark:text-slate-500" size={15} />
        </div>
      </div>
    </button>
  );
};

export default CurrentOrderBanner;
