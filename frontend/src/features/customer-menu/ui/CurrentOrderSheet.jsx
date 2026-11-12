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
          <div className="flex items-center gap-2 text-[#C17C4A]">
            <ReceiptText size={20} />
            <h3 className="truncate text-lg font-black text-[#2D1D16] dark:text-[#F7F0E7]">
              Đơn #{order.id}
            </h3>
          </div>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <span className={`rounded-full px-3 py-1 text-xs font-bold ${status.classes}`}>
              {status.label}
            </span>
            {order.createdAt && (
              <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-[#6B5D55] dark:text-[#BDB0A4]">
                <Clock3 size={12} />
                {fmtDateTime(order.createdAt)}
              </span>
            )}
          </div>
        </div>

        <button
          type="button"
          onClick={onClose}
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#F7F0E7] text-[#6B5D55] transition-colors hover:bg-[#EAE0D5] dark:bg-[#2A221E] dark:text-[#BDB0A4] dark:hover:bg-[#352C26]"
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
              <div
                key={item.id}
                className="rounded-2xl border border-gray-100 bg-gray-50 p-4 transition-colors dark:border-slate-800 dark:bg-slate-950/60"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <p className="line-clamp-2 text-sm font-black text-gray-900 dark:text-white">
                      {getItemName(item)}
                      <span className="ml-1 text-orange-600 dark:text-orange-300">
                        x{item.quantity}
                      </span>
                    </p>
                    {item.options?.length > 0 && (
                      <p className="mt-1 text-[11px] font-medium leading-relaxed text-gray-500 dark:text-gray-400">
                        {item.options
                          .map((opt) => `${opt.optionName}: ${opt.optionValueName}`)
                          .join(', ')}
                      </p>
                    )}
                    {item.notes && (
                      <p className="mt-2 rounded-xl bg-white px-3 py-2 text-[11px] font-medium text-gray-500 dark:bg-slate-900 dark:text-gray-400">
                        {item.notes}
                      </p>
                    )}
                  </div>

                  <div className="shrink-0 text-right">
                    <span
                      className={`rounded-full px-2 py-1 text-xs font-bold ${itemStatus.classes}`}
                    >
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

      <div className="safe-bottom mt-5 space-y-4 border-t border-[#E8DCD1] pt-4 transition-colors dark:border-[#352C26]">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-[#6B5D55] dark:text-[#BDB0A4]">
            Tạm tính
          </span>
          <span className="text-2xl font-black text-[#56372B] dark:text-[#D8986A]">
            {fmtVND(getOrderFinalAmount(order))}
          </span>
        </div>

        {/* Payment Request Status Banner or Button */}
        {items.length > 0 && order.status !== 'COMPLETED' && order.status !== 'CANCELLED' && (
          <div>
            {isAwaitingPayment ? (
              <div className="flex items-center justify-center gap-2 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-center text-xs font-bold text-amber-800 dark:border-amber-900/50 dark:bg-amber-950/40 dark:text-amber-300">
                <CheckCircle2 size={16} className="shrink-0 text-amber-600 dark:text-amber-400" />
                <span>Đã gọi thanh toán. Nhân viên sẽ tới bàn của bạn ngay!</span>
              </div>
            ) : (
              <button
                type="button"
                onClick={handleRequestClick}
                disabled={requesting}
                className="flex w-full items-center justify-center gap-2 rounded-2xl bg-[#56372B] hover:bg-[#432A21] dark:bg-[#C17C4A] dark:hover:bg-[#A86537] py-3.5 text-sm font-black text-white shadow-lg shadow-[#56372B]/20 dark:shadow-[#C17C4A]/25 transition-all active:scale-[0.98] disabled:cursor-not-allowed disabled:bg-slate-300 disabled:shadow-none dark:disabled:bg-slate-800"
              >
                {requesting ? (
                  <Loader2 size={18} className="animate-spin" />
                ) : (
                  <CreditCard size={18} />
                )}
                YÊU CẦU THANH TOÁN
              </button>
            )}
          </div>
        )}
      </div>
    </SharedModal>
  );
};

export default CurrentOrderSheet;
