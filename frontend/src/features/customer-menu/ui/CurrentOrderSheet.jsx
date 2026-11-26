import React, { useState } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Clock3,
  CreditCard,
  Loader2,
  QrCode,
  ReceiptText,
  X,
} from 'lucide-react';

import { fmtDateTime, fmtVND } from '@shared/lib/formatters.js';
import { getOrderStatusMeta, getItemStatusMeta } from '@entities/order';
import SharedModal from '@shared/ui/SharedModal.jsx';
import {
  canCustomerRequestPayment,
  isCustomerSelfCheckoutAvailable,
} from '../lib/selfCheckoutHelpers.js';
import SelfCheckoutPanel from './SelfCheckoutPanel.jsx';

const getItemName = (item) =>
  item.itemNameSnapshot || item.menuItem?.name || item.combo?.name || 'Món đã gọi';

const getLineTotal = (item) => {
  if (item.lineTotal != null) return Number(item.lineTotal);
  if (item.combo?.price) return Number(item.combo.price) * Number(item.quantity || 0);
  return Number(item.unitPrice || 0) * Number(item.quantity || 0);
};

const CurrentOrderSheetContent = ({
  isOpen,
  order,
  checkout,
  onClose,
  onRequestPayment,
  tableStatus,
  restaurantSettings,
}) => {
  const [requesting, setRequesting] = useState(false);
  const [justRequested, setJustRequested] = useState(false);

  const selfCheckoutAvailable = isCustomerSelfCheckoutAvailable(restaurantSettings);
  const isOrderPaid = order.paymentStatus === 'PAID';

  const status = getOrderStatusMeta(order.status);
  const items = order.items || order.orderItems || [];
  const canRequestPayment = canCustomerRequestPayment(order);
  const paymentBlockedByKitchen = !canRequestPayment;
  const isAwaitingPayment =
    order.status === 'AWAITING_PAYMENT' ||
    (canRequestPayment && (tableStatus === 'WAITING_FOR_PAYMENT' || justRequested));

  const handleRequestClick = async () => {
    if (requesting || isAwaitingPayment || paymentBlockedByKitchen || !onRequestPayment) return;
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
      closeOnBackdrop={checkout.payosStatus !== 'waiting'}
      ariaLabel={`Chi tiết đơn ${order.id}`}
      className="!max-h-[85vh] !max-w-md !p-6"
    >
      <div className="mb-5 flex items-start justify-between gap-4">
        <div className="min-w-0">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-md shadow-indigo-500/25">
              <ReceiptText size={18} />
            </div>
            <h3 className="truncate font-mono text-base font-black text-slate-900 dark:text-white">
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
          className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-slate-100 text-slate-500 transition-colors hover:bg-slate-200 active:scale-95 dark:bg-slate-800 dark:text-slate-400 dark:hover:bg-slate-700"
          aria-label="Đóng chi tiết đơn"
          title="Đóng chi tiết đơn"
        >
          <X size={16} />
        </button>
      </div>

      <div className="custom-scrollbar flex-1 overflow-y-auto pr-1">
        {selfCheckoutAvailable && checkout.isCheckoutOpen ? (
          <SelfCheckoutPanel
            order={order}
            checkout={checkout}
            onBack={() => checkout.setIsCheckoutOpen(false)}
            onRequestStaffPayment={handleRequestClick}
            isAwaitingStaffPayment={isAwaitingPayment}
            requestingStaff={requesting}
          />
        ) : (
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
                        <span className="ml-1 font-mono text-indigo-600 dark:text-indigo-400">
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
                        <p className="mt-2 rounded-xl border border-slate-200/60 bg-white px-3 py-1.5 text-[11px] font-medium text-slate-600 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300">
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
                      <p className="mt-2 font-mono text-xs font-black text-slate-900 dark:text-white">
                        {fmtVND(getLineTotal(item))}
                      </p>
                    </div>
                  </div>
                </div>
              );
            })}

            {items.length === 0 && (
              <div className="rounded-2xl border border-dashed border-slate-200/80 py-8 text-center text-xs font-semibold italic text-slate-400 dark:border-slate-800/80 dark:text-slate-500">
                Đơn hiện tại chưa có món.
              </div>
            )}
          </div>
        )}
      </div>

      {(!selfCheckoutAvailable || !checkout.isCheckoutOpen) && (
        <div className="safe-bottom mt-5 space-y-4 border-t border-slate-200/80 pt-4 transition-colors dark:border-slate-800/80">
          {(checkout.summary.discountAmount > 0 ||
            checkout.summary.serviceChargeAmount > 0 ||
            checkout.summary.taxAmount > 0) && (
            <div className="space-y-1.5 text-xs">
              <div className="flex items-center justify-between text-slate-500 dark:text-slate-400">
                <span>Tạm tính món</span>
                <span className="font-mono font-semibold text-slate-700 dark:text-slate-200">
                  {fmtVND(checkout.summary.subtotalAmount)}
                </span>
              </div>
              {checkout.summary.promotionDiscountAmount > 0 && (
                <div className="flex items-center justify-between gap-2 text-indigo-600 dark:text-indigo-400">
                  <span className="truncate font-semibold">
                    Khuyến mãi{' '}
                    {checkout.summary.promotionName
                      ? `(${checkout.summary.promotionName}${checkout.summary.promotionPercent ? ` -${checkout.summary.promotionPercent}%` : ''})`
                      : 'tự động'}
                  </span>
                  <span className="shrink-0 font-mono font-bold">
                    -{fmtVND(checkout.summary.promotionDiscountAmount)}
                  </span>
                </div>
              )}
              {checkout.summary.voucherDiscountAmount > 0 && (
                <div className="flex items-center justify-between gap-2 text-emerald-600 dark:text-emerald-400">
                  <span className="truncate font-semibold">
                    Voucher {order.voucherCode ? `(${order.voucherCode})` : ''}
                  </span>
                  <span className="shrink-0 font-mono font-bold">
                    -{fmtVND(checkout.summary.voucherDiscountAmount)}
                  </span>
                </div>
              )}
              {checkout.summary.serviceChargeAmount > 0 && (
                <div className="flex items-center justify-between gap-2 text-slate-500 dark:text-slate-400">
                  <span>
                    Phí dịch vụ
                    {checkout.summary.serviceChargePercent > 0
                      ? ` (${checkout.summary.serviceChargePercent}%)`
                      : ''}
                  </span>
                  <span className="shrink-0 font-mono font-semibold text-slate-700 dark:text-slate-200">
                    +{fmtVND(checkout.summary.serviceChargeAmount)}
                  </span>
                </div>
              )}
              {checkout.summary.taxAmount > 0 && (
                <div className="flex items-center justify-between gap-2 text-slate-500 dark:text-slate-400">
                  <span>
                    Thuế VAT
                    {checkout.summary.taxPercent > 0 ? ` (${checkout.summary.taxPercent}%)` : ''}
                  </span>
                  <span className="shrink-0 font-mono font-semibold text-slate-700 dark:text-slate-200">
                    +{fmtVND(checkout.summary.taxAmount)}
                  </span>
                </div>
              )}
            </div>
          )}
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
              {checkout.summary.discountAmount > 0 ||
              checkout.summary.serviceChargeAmount > 0 ||
              checkout.summary.taxAmount > 0
                ? 'Tổng thanh toán'
                : 'Tạm tính'}
            </span>
            <span className="font-mono text-2xl font-black tracking-tight text-indigo-600 dark:text-indigo-400">
              {fmtVND(checkout.summary.finalAmount)}
            </span>
          </div>

          {items.length > 0 && isOrderPaid && (
            <div className="flex items-center justify-center gap-2 rounded-2xl border border-emerald-200/70 bg-emerald-50/80 px-4 py-3 text-center text-xs font-bold text-emerald-700 dark:border-emerald-800/40 dark:bg-emerald-950/40 dark:text-emerald-300">
              <CheckCircle2 size={16} className="shrink-0" />
              <span>Đơn hàng đã được thanh toán. Bếp đang hoàn tất các món của bạn!</span>
            </div>
          )}

          {items.length > 0 &&
            !isOrderPaid &&
            order.status !== 'COMPLETED' &&
            order.status !== 'CANCELLED' && (
            <div className="space-y-2.5">
              {paymentBlockedByKitchen && !isAwaitingPayment && (
                <div className="flex items-start gap-2 rounded-2xl border border-amber-200/70 bg-amber-50/80 p-3 text-xs font-semibold text-amber-800 dark:border-amber-800/40 dark:bg-amber-950/40 dark:text-amber-300">
                  <AlertCircle size={16} className="mt-0.5 shrink-0" />
                  <span>
                    Bếp vẫn đang chế biến món. Bạn có thể yêu cầu thanh toán ngay khi các món hoàn
                    tất.
                  </span>
                </div>
              )}

              {selfCheckoutAvailable ? (
                <>
                  <button
                    type="button"
                    onClick={() => checkout.setIsCheckoutOpen(true)}
                    disabled={paymentBlockedByKitchen}
                    className="flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-indigo-600 to-violet-600 py-3.5 text-xs font-bold uppercase tracking-wider text-white shadow-lg shadow-indigo-600/25 transition-all hover:from-indigo-500 hover:to-violet-500 active:scale-[0.98] disabled:cursor-not-allowed disabled:from-slate-300 disabled:to-slate-300 disabled:text-slate-500 disabled:shadow-none dark:disabled:from-slate-800 dark:disabled:to-slate-800 dark:disabled:text-slate-400"
                  >
                    <QrCode size={18} />
                    Tự thanh toán VietQR
                  </button>
                  {isAwaitingPayment ? (
                    <div className="flex items-center justify-center gap-2 rounded-2xl border border-amber-200/60 bg-amber-50/80 px-4 py-2.5 text-center text-xs font-bold text-amber-800 dark:border-amber-800/40 dark:bg-amber-950/40 dark:text-amber-300">
                      <CheckCircle2
                        size={15}
                        className="shrink-0 text-amber-600 dark:text-amber-400"
                      />
                      <span>Đã gọi nhân viên. Bạn vẫn có thể tự quét VietQR ở trên!</span>
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={handleRequestClick}
                      disabled={requesting || paymentBlockedByKitchen}
                      className="flex w-full items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white py-3 text-xs font-bold text-slate-600 transition hover:bg-slate-50 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
                    >
                      {requesting ? (
                        <Loader2 size={16} className="animate-spin" />
                      ) : (
                        <CreditCard size={16} />
                      )}
                      Gọi nhân viên hỗ trợ thanh toán
                    </button>
                  )}
                </>
              ) : isAwaitingPayment ? (
                <div className="flex items-center justify-center gap-2 rounded-2xl border border-amber-200/60 bg-amber-50/80 px-4 py-3 text-center text-xs font-bold text-amber-800 dark:border-amber-800/40 dark:bg-amber-950/40 dark:text-amber-300">
                  <CheckCircle2
                    size={16}
                    className="shrink-0 text-amber-600 dark:text-amber-400"
                  />
                  <span>Đã gọi thanh toán. Nhân viên sẽ tới bàn của bạn ngay!</span>
                </div>
              ) : (
                <button
                  type="button"
                  onClick={handleRequestClick}
                  disabled={requesting || paymentBlockedByKitchen}
                  className="flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-emerald-600 to-teal-600 py-3.5 text-xs font-bold uppercase tracking-wider text-white shadow-lg shadow-emerald-600/25 transition-all hover:from-emerald-500 hover:to-teal-500 active:scale-[0.98] disabled:cursor-not-allowed disabled:from-slate-300 disabled:to-slate-300 disabled:text-slate-500 disabled:shadow-none dark:disabled:from-slate-800 dark:disabled:to-slate-800 dark:disabled:text-slate-400"
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
      )}
    </SharedModal>
  );
};

const CurrentOrderSheet = (props) => {
  if (!props.isOpen || !props.order) return null;
  return (
    <CurrentOrderSheetContent
      key={`${props.tableCode || 'none'}-${props.order.id}`}
      {...props}
    />
  );
};

export default CurrentOrderSheet;
