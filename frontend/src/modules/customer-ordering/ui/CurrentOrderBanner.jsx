import React from 'react';
import { ChevronRight, Clock3, ReceiptText } from 'lucide-react';

import { fmtVND } from '@shared/lib/formatters.js';
import { getOrderStatusMeta } from '@entities/order/lib/orderStatus.js';

const countItems = (order) => (order?.orderItems || [])
  .reduce((sum, item) => sum + (Number(item.quantity) || 0), 0);

const CurrentOrderBanner = ({ order, onClick }) => {
  if (!order) return null;

  const status = getOrderStatusMeta(order.status);
  const itemCount = countItems(order);

  return (
    <button
      type="button"
      onClick={onClick}
      className="w-full rounded-xl border border-orange-100 bg-white p-2.5 text-left shadow-sm transition-all active:scale-[0.99] dark:border-slate-800 dark:bg-slate-900"
    >
      <div className="flex items-center gap-2.5">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-orange-50 text-orange-600 dark:bg-orange-500/10 dark:text-orange-300">
          <ReceiptText size={17} />
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <p className="truncate text-[13px] font-black text-gray-900 dark:text-white">
              Đơn #{order.id}
            </p>
            <span className={`shrink-0 rounded-full border px-2 py-0.5 text-[9px] font-bold ${status.classes}`}>
              {status.label}
            </span>
          </div>
          <div className="mt-0.5 flex items-center gap-1.5 text-[10px] font-semibold text-gray-500 dark:text-gray-400">
            <Clock3 size={11} />
            <span className="truncate">{status.helper}</span>
            <span className="shrink-0">•</span>
            <span className="shrink-0">{itemCount} món</span>
          </div>
        </div>

        <div className="shrink-0 text-right">
          <p className="text-[13px] font-black text-orange-600 dark:text-orange-300">
            {fmtVND(order.totalAmount)}
          </p>
          <ChevronRight className="ml-auto mt-0.5 text-gray-300 dark:text-slate-600" size={15} />
        </div>
      </div>
    </button>
  );
};

export default CurrentOrderBanner;
