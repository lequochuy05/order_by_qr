import React, { useState } from 'react';
import { Clock3, ReceiptText, X, CreditCard, CheckCircle2, Loader2 } from 'lucide-react';

import { fmtDateTime, fmtVND } from '@shared/lib/formatters.js';
import { getOrderStatusMeta, getItemStatusMeta } from '@entities/order/lib/orderStatus.js';
import { getOrderFinalAmount } from '@entities/order/lib/orderMoney.js';
import SharedModal from '@shared/ui/SharedModal.jsx';

const getItemName = (item) =>
  item.itemNameSnapshot || item.menuItem?.name || item.combo?.name || 'Món đã gọi';

const getLineTotal = (item) => {
  if (item.lineTotal != null) return Number(item.lineTotal);
  if (item.combo?.price) return Number(item.combo.price) * Number(item.quantity || 0);
  return Number(item.unitPrice || 0) * Number(item.quantity || 0);
};

const CurrentOrderSheet = ({
  isOpen,
  order,
  onClose,
  onRequestPayment,
  tableStatus,
}) => {
  const [requesting, setRequesting] = useState(false);
  const [justRequested, setJustRequested] = useState(false);

  if (!isOpen || !order) return null;

  const status = getOrderStatusMeta(order.status);
  const items = order.items || order.orderItems || [];
  const isAwaitingPayment =
    order.status === 'AWAITING_PAYMENT' ||
    tableStatus === 'WAITING_FOR_PAYMENT' ||
    justRequested;

  const handleRequestClick = async () => {
    if (requesting || isAwaitingPayment || !onRequestPayment) return;
    setRequesting(true);
    try {
      await onRequestPayment();
      setJustRequested(true);
    } catch {
      // Handled by parent error handler
    } finally {
      setRequesting(false);
    }
  };

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      placement="bottom"
      closeOnBackdrop
      ariaLabel={`Chi tiết đơn ${order.id}`}
      className="!max-h-[85vh] !max-w-md !p-6"
    >
      <div className="mb-5 flex items-start justify-between gap-4">
        <div className="min-w-0">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-md shadow-indigo-500/25">
              <ReceiptText size={18} />
            </div>
            <h3 className="truncate text-base font-black font-mono text-slate-900 dark:text-white">
              Đơn #{order.id}
            </h3>
          </div>
          <div className="mt-2.5 flex flex-wrap items-center gap-2">
            <span className={`rounded-full px-3 py-0.5 text-xs font-bold ${status.classes}`}>
              {status.label}
            </span>
            {order.createdAt && (
              <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-slate-400 dark:text-slate-500">
                <Clock3 size={12} />
                {fmtDateTime(order.createdAt)}
              </span>
            )}
          </div>
        </div>

        <button
          type="button"
          onClick={onClose}
          className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-slate-100 text-slate-500 transition-colors hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-400 dark:hover:bg-slate-700 active:scale-95"
          aria-label="Đóng chi tiết đơn"
          title="Đóng chi tiết đơn"
        >
          <X size={16} />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto pr-1 custom-scrollbar">
        <div className="space-y-3">
          {items.map((item) => {
            const itemStatus = getItemStatusMeta(item.status);
            return (
              <div
                key={item.id}
                className="rounded-2xl border border-slate-200/80 bg-slate-50/60 p-4 transition-colors dark:border-slate-800/80 dark:bg-slate-900/60"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <p className="line-clamp-2 text-xs font-bold text-slate-900 dark:text-white">
                      {getItemName(item)}
                      <span className="ml-1 text-indigo-600 dark:text-indigo-400 font-mono">
                        x{item.quantity}
                      </span>
                    </p>
                    {item.options?.length > 0 && (
                      <p className="mt-1 text-[11px] font-medium leading-relaxed text-slate-400 dark:text-slate-500">
                        {item.options
                          .map((opt) => `${opt.optionName}: ${opt.optionValueName}`)
                          .join(', ')}
                      </p>
                    )}
                    {item.notes && (
                      <p className="mt-2 rounded-xl bg-white px-3 py-1.5 text-[11px] font-medium text-slate-600 border border-slate-200/60 dark:bg-slate-900 dark:border-slate-800 dark:text-slate-300">
                        {item.notes}
                      </p>
                    )}
                  </div>

                  <div className="shrink-0 text-right">
                    <span
                      className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${itemStatus.classes}`}
                    >
                      {itemStatus.label}
                    </span>
                    <p className="mt-2 text-xs font-black font-mono text-slate-900 dark:text-white">
                      {fmtVND(getLineTotal(item))}
                    </p>
                  </div>
                </div>
              </div>
            );
          })}

          {items.length === 0 && (
            <div className="rounded-2xl border border-dashed border-slate-200/80 py-8 text-center text-xs font-semibold text-slate-400 dark:border-slate-800/80 dark:text-slate-500 italic">
              Đơn hiện tại chưa có món.
            </div>
          )}
        </div>
      </div>

      <div className="safe-bottom mt-5 space-y-4 border-t border-slate-200/80 pt-4 transition-colors dark:border-slate-800/80">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
            Tạm tính
          </span>
          <span className="text-2xl font-black font-mono tracking-tight text-indigo-600 dark:text-indigo-400">
            {fmtVND(getOrderFinalAmount(order))}
          </span>
        </div>

        {/* Payment Request Status Banner or Button */}
        {items.length > 0 && order.status !== 'COMPLETED' && order.status !== 'CANCELLED' && (
          <div>
            {isAwaitingPayment ? (
              <div className="flex items-center justify-center gap-2 rounded-2xl border border-amber-200/60 bg-amber-50/80 px-4 py-3 text-center text-xs font-bold text-amber-800 dark:border-amber-800/40 dark:bg-amber-950/40 dark:text-amber-300">
                <CheckCircle2 size={16} className="shrink-0 text-amber-600 dark:text-amber-400" />
                <span>Đã gọi thanh toán. Nhân viên sẽ tới bàn của bạn ngay!</span>
              </div>
            ) : (
              <button
                type="button"
                onClick={handleRequestClick}
                disabled={requesting}
                className="flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 py-3.5 text-xs font-bold uppercase tracking-wider text-white shadow-lg shadow-emerald-600/25 transition-all active:scale-[0.98] disabled:cursor-not-allowed disabled:bg-slate-300 disabled:shadow-none dark:disabled:bg-slate-800"
              >
                {requesting ? (
                  <Loader2 size={18} className="animate-spin" />
                ) : (
                  <CreditCard size={18} />
                )}
                Yêu cầu thanh toán
              </button>
            )}
          </div>
        )}
      </div>
    </SharedModal>
  );
};

export default CurrentOrderSheet;
