import React from 'react';
import { Clock3, ReceiptText, X } from 'lucide-react';

import { fmtDateTime, fmtVND } from '../../../utils/formatters.js';
import { getOrderStatusMeta, getItemStatusMeta } from '../../../utils/orderStatus.js';

const getItemName = (item) => item.menuItem?.name || item.combo?.name || 'Món đã gọi';

const getLineTotal = (item) => {
  if (item.combo?.price) return Number(item.combo.price) * Number(item.quantity || 0);
  return Number(item.unitPrice || 0) * Number(item.quantity || 0);
};

const CurrentOrderSheet = ({ isOpen, order, onClose }) => {
  if (!isOpen || !order) return null;

  const status = getOrderStatusMeta(order.status);
  const items = order.orderItems || [];

  return (
    <div
      className="fixed inset-0 z-50 flex items-end bg-black/60 animate-in fade-in duration-300"
      onClick={onClose}
    >
      <div
        className="mx-auto flex max-h-[85vh] w-full max-w-md flex-col rounded-t-[2rem] bg-white p-6 shadow-2xl transition-colors animate-in slide-in-from-bottom duration-500 dark:bg-slate-900"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-5 flex items-start justify-between gap-4">
          <div className="min-w-0">
            <div className="flex items-center gap-2 text-orange-600 dark:text-orange-300">
              <ReceiptText size={20} />
              <h3 className="truncate text-lg font-black text-gray-900 dark:text-white">
                Đơn #{order.id}
              </h3>
            </div>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <span className={`rounded-full px-3 py-1 text-xs font-bold ${status.classes}`}>
                {status.label}
              </span>
              {order.createdAt && (
                <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-gray-500 dark:text-gray-400">
                  <Clock3 size={12} />
                  {fmtDateTime(order.createdAt)}
                </span>
              )}
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gray-100 text-gray-600 transition-colors hover:bg-gray-200 dark:bg-slate-800 dark:text-gray-300 dark:hover:bg-slate-700"
            aria-label="Đóng chi tiết đơn"
            title="Đóng chi tiết đơn"
          >
            <X size={18} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto pr-1">
          <div className="space-y-3">
            {items.map((item) => {
              const itemStatus = getItemStatusMeta(item.status);
              return (
                <div key={item.id} className="rounded-2xl border border-gray-100 bg-gray-50 p-4 transition-colors dark:border-slate-800 dark:bg-slate-950/60">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <p className="line-clamp-2 text-sm font-black text-gray-900 dark:text-white">
                        {getItemName(item)}
                        <span className="ml-1 text-orange-600 dark:text-orange-300">x{item.quantity}</span>
                      </p>
                      {item.options?.length > 0 && (
                        <p className="mt-1 text-[11px] font-medium leading-relaxed text-gray-500 dark:text-gray-400">
                          {item.options.map(opt => `${opt.optionName}: ${opt.optionValueName}`).join(', ')}
                        </p>
                      )}
                      {item.notes && (
                        <p className="mt-2 rounded-xl bg-white px-3 py-2 text-[11px] font-medium text-gray-500 dark:bg-slate-900 dark:text-gray-400">
                          {item.notes}
                        </p>
                      )}
                    </div>

                    <div className="shrink-0 text-right">
                      <span className={`rounded-full px-2 py-1 text-[10px] font-bold ${itemStatus.classes}`}>
                        {itemStatus.label}
                      </span>
                      <p className="mt-2 text-xs font-black text-gray-800 dark:text-gray-200">
                        {fmtVND(getLineTotal(item))}
                      </p>
                    </div>
                  </div>
                </div>
              );
            })}

            {items.length === 0 && (
              <div className="rounded-2xl border border-dashed border-gray-200 py-8 text-center text-xs font-semibold text-gray-400 dark:border-slate-800 dark:text-gray-500">
                Đơn hiện tại chưa có món.
              </div>
            )}
          </div>
        </div>

        <div className="mt-5 border-t border-gray-100 pt-4 transition-colors dark:border-slate-800">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400">
              Tạm tính
            </span>
            <span className="text-2xl font-black text-orange-600 dark:text-orange-300">
              {fmtVND(order.totalAmount)}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CurrentOrderSheet;
