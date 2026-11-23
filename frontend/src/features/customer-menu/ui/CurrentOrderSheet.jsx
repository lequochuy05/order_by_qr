import React, { useState } from 'react';
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  Clock3,
  CreditCard,
  Loader2,
  QrCode,
  ReceiptText,
  RefreshCw,
  Tag,
  X,
  XCircle,
} from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';

import { fmtDateTime, fmtVND } from '@shared/lib/formatters.js';
import { getOrderStatusMeta, getItemStatusMeta } from '@entities/order/lib/orderStatus.js';
import SharedModal from '@shared/ui/SharedModal.jsx';
import {
  canCustomerRequestPayment,
  isCustomerSelfCheckoutAvailable,
} from '../lib/selfCheckoutHelpers.js';
import useCustomerSelfCheckout from '../model/useCustomerSelfCheckout.js';

const getItemName = (item) =>
  item.itemNameSnapshot || item.menuItem?.name || item.combo?.name || 'Món đã gọi';

const getLineTotal = (item) => {
  if (item.lineTotal != null) return Number(item.lineTotal);
  if (item.combo?.price) return Number(item.combo.price) * Number(item.quantity || 0);
  return Number(item.unitPrice || 0) * Number(item.quantity || 0);
};

const formatCountdown = (seconds) => {
  const safe = Math.max(0, Number(seconds || 0));
  const minutes = Math.floor(safe / 60);
  const remaining = safe % 60;
  return `${minutes.toString().padStart(2, '0')}:${remaining.toString().padStart(2, '0')}`;
};

const SelfCheckoutPanel = ({
  order,
  checkout,
  onBack,
  onRequestStaffPayment,
  isAwaitingStaffPayment,
  requestingStaff,
}) => {
  const {
    voucherCode,
    appliedVoucher,
    voucherLoading,
    voucherError,
    voucherRequiresApply,
    summary,
    unfinishedItems,
    canCreateQr,
    payosData,
    payosStatus,
    payosLoading,
    payosError,
    timeLeft,
    handleVoucherInputChange,
    handleApplyVoucher,
    handleClearVoucher,
    handleCreateQr,
    handleCancelQr,
  } = checkout;

  const isLockedWithActiveQr = payosStatus === 'waiting' || payosStatus === 'success';

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={onBack}
          disabled={payosStatus === 'waiting' || payosLoading}
          className="inline-flex items-center gap-1.5 text-xs font-bold text-slate-500 transition hover:text-slate-800 disabled:cursor-not-allowed disabled:opacity-40 dark:text-slate-400 dark:hover:text-slate-200"
        >
          <ArrowLeft size={14} />
          Xem lại danh sách món
        </button>
        <span className="rounded-full bg-indigo-50 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-300">
          Tự thanh toán VietQR
        </span>
      </div>

      {/* Voucher box */}
      {payosStatus !== 'success' && (
        <div className="space-y-2 rounded-2xl border border-slate-200/80 bg-slate-50/60 p-4 dark:border-slate-800/80 dark:bg-slate-900/60">
          <label className="block text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
            Mã giảm giá (Voucher)
          </label>
          <div className="flex gap-2">
            <div className="relative flex-1">
              <Tag
                size={15}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500"
              />
              <input
                type="text"
                disabled={isLockedWithActiveQr || voucherLoading}
                placeholder="NHẬP MÃ GIẢM GIÁ"
                value={voucherCode}
                onChange={(e) => handleVoucherInputChange(e.target.value.toUpperCase())}
                className="w-full rounded-xl border border-slate-200/80 bg-white py-2.5 pl-9 pr-8 text-xs font-bold uppercase text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700/80 dark:bg-slate-950 dark:text-slate-100"
              />
              {voucherCode && !isLockedWithActiveQr && (
                <button
                  type="button"
                  onClick={handleClearVoucher}
                  aria-label="Xóa mã giảm giá"
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
                >
                  <X size={14} />
                </button>
              )}
            </div>
            <button
              type="button"
              onClick={handleApplyVoucher}
              disabled={isLockedWithActiveQr || voucherLoading || !voucherCode.trim()}
              className="rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-4 text-xs font-bold text-white shadow-md shadow-indigo-600/20 transition hover:from-indigo-500 hover:to-violet-500 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {voucherLoading ? <Loader2 size={15} className="animate-spin" /> : 'Áp dụng'}
            </button>
          </div>
          {voucherError && (
            <p className="text-xs font-semibold text-rose-500">{voucherError}</p>
          )}
          {voucherRequiresApply && !voucherError && (
            <p className="text-xs font-semibold text-amber-600 dark:text-amber-400">
              Vui lòng bấm &quot;Áp dụng&quot; để kiểm tra mã giảm giá.
            </p>
          )}
          {appliedVoucher?.applicable && !voucherRequiresApply && (
            <p className="flex items-center gap-1.5 font-mono text-xs font-bold text-emerald-600 dark:text-emerald-400">
              <CheckCircle2 size={13} /> Đã áp dụng {appliedVoucher.code}: -
              {fmtVND(summary.voucherDiscountAmount || appliedVoucher.appliedDiscountAmount)}
            </p>
          )}
        </div>
      )}

      {/* Bill breakdown */}
      <div className="space-y-2 rounded-2xl border border-slate-200/80 bg-slate-50/80 p-4 text-xs dark:border-slate-800/80 dark:bg-slate-950/60">
        <div className="flex justify-between text-slate-500 dark:text-slate-400">
          <span>Tạm tính:</span>
          <span className="font-mono font-bold text-slate-700 dark:text-slate-200">
            {fmtVND(summary.subtotalAmount)}
          </span>
        </div>
        {summary.promotionDiscountAmount > 0 && (
          <div className="flex justify-between gap-2 text-indigo-600 dark:text-indigo-400">
            <span className="truncate font-semibold">
              Khuyến mãi{' '}
              {summary.promotionName
                ? `(${summary.promotionName}${summary.promotionPercent ? ` -${summary.promotionPercent}%` : ''})`
                : 'tự động'}
              :
            </span>
            <span className="shrink-0 font-mono font-bold">
              -{fmtVND(summary.promotionDiscountAmount)}
            </span>
          </div>
        )}
        {summary.voucherDiscountAmount > 0 && (
          <div className="flex justify-between gap-2 text-emerald-600 dark:text-emerald-400">
            <span className="truncate font-semibold">
              Giảm giá voucher
              {appliedVoucher?.code
                ? ` (${appliedVoucher.code})`
                : order?.voucherCode
                  ? ` (${order.voucherCode})`
                  : ''}
              :
            </span>
            <span className="shrink-0 font-mono font-bold">
              -{fmtVND(summary.voucherDiscountAmount)}
            </span>
          </div>
        )}
        <div className="flex items-center justify-between border-t border-dashed border-slate-200/80 pt-2.5 dark:border-slate-800">
          <span className="font-bold text-slate-800 dark:text-slate-100">Cần thanh toán:</span>
          <span className="font-mono text-xl font-black text-indigo-600 dark:text-indigo-400">
            {fmtVND(summary.finalAmount)}
          </span>
        </div>
      </div>

      {/* Kitchen unfinished warning if applicable */}
      {(unfinishedItems || !canCreateQr) && (
        <div className="flex items-start gap-2 rounded-2xl border border-amber-200/70 bg-amber-50/80 p-3.5 text-xs font-semibold text-amber-800 dark:border-amber-800/40 dark:bg-amber-950/40 dark:text-amber-300">
          <AlertCircle size={16} className="mt-0.5 shrink-0" />
          <span>
            Bếp vẫn còn món đang chế biến. Bạn có thể thanh toán hoặc yêu cầu tính tiền ngay khi
            các món hoàn tất.
          </span>
        </div>
      )}

      {/* PayOS QR Area */}
      {(payosStatus === 'idle' || payosStatus === 'error') && (
        <div className="space-y-2.5">
          <button
            type="button"
            onClick={handleCreateQr}
            disabled={payosLoading || !canCreateQr || voucherRequiresApply}
            className="flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-indigo-600 to-violet-600 py-3.5 text-xs font-black uppercase tracking-wider text-white shadow-lg shadow-indigo-600/25 transition hover:from-indigo-500 hover:to-violet-500 active:scale-[0.98] disabled:cursor-not-allowed disabled:bg-slate-300 disabled:shadow-none dark:disabled:bg-slate-800"
          >
            {payosLoading ? <Loader2 size={18} className="animate-spin" /> : <QrCode size={18} />}
            Tạo mã VietQR thanh toán
          </button>
          {payosError && (
            <p className="text-center text-xs font-semibold text-rose-500">{payosError}</p>
          )}
        </div>
      )}

      {(payosStatus === 'waiting' || payosStatus === 'expired') && payosData && (
        <div className="relative space-y-3 overflow-hidden rounded-2xl border-2 border-dashed border-indigo-200/80 bg-slate-50/60 p-5 text-center dark:border-indigo-500/30 dark:bg-slate-950/60">
          {payosStatus === 'expired' && (
            <div className="absolute inset-0 z-10 flex flex-col items-center justify-center bg-white/90 p-4 backdrop-blur-[2px] dark:bg-slate-950/90">
              <XCircle size={32} className="mb-2 text-rose-500" />
              <p className="text-sm font-bold text-rose-600">Mã VietQR đã hết hạn</p>
              <p className="mb-4 mt-1 text-xs text-slate-500 dark:text-slate-400">
                Vui lòng tạo mã mới để tiếp tục thanh toán
              </p>
              <button
                type="button"
                onClick={handleCreateQr}
                disabled={payosLoading}
                className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-5 py-2.5 text-xs font-bold text-white shadow-md shadow-indigo-600/25 transition hover:from-indigo-500 hover:to-violet-500 active:scale-95"
              >
                {payosLoading ? (
                  <Loader2 size={15} className="animate-spin" />
                ) : (
                  <RefreshCw size={15} />
                )}
                Tạo mã mới
              </button>
            </div>
          )}

          <div className="flex justify-center">
            <div className="rounded-2xl border border-slate-200/80 bg-white p-3 shadow-sm dark:border-slate-700">
              <QRCodeSVG value={payosData.qrCode} size={196} level="H" includeMargin />
            </div>
          </div>

          <div className="mx-auto max-w-xs space-y-1.5 rounded-2xl border border-indigo-100 bg-white/90 px-4 py-2.5 shadow-xs dark:border-indigo-500/20 dark:bg-slate-900/90">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-slate-500 dark:text-slate-400">
                Số tiền thanh toán:
              </span>
              <span className="font-mono text-base font-black text-indigo-600 dark:text-indigo-400">
                {fmtVND(summary.finalAmount)}
              </span>
            </div>
            {summary.discountAmount > 0 && (
              <div className="flex flex-wrap items-center justify-center gap-1.5 border-t border-dashed border-slate-200/80 pt-1.5 text-[11px] font-bold text-emerald-600 dark:border-slate-800 dark:text-emerald-400">
                <span>Đã giảm -{fmtVND(summary.discountAmount)}</span>
                {summary.promotionDiscountAmount > 0 && (
                  <span className="rounded-full bg-indigo-50 px-2 py-0.5 text-[10px] font-bold text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-300">
                    KM -{fmtVND(summary.promotionDiscountAmount)}
                  </span>
                )}
                {summary.voucherDiscountAmount > 0 && (
                  <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-bold text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300">
                    Voucher -{fmtVND(summary.voucherDiscountAmount)}
                  </span>
                )}
              </div>
            )}
          </div>

          <div className="flex items-center justify-center">
            <span
              className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 font-mono text-xs font-bold ${
                timeLeft < 60
                  ? 'animate-pulse bg-rose-100 text-rose-600 dark:bg-rose-500/20 dark:text-rose-400'
                  : 'bg-indigo-100 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300'
              }`}
            >
              <Loader2 size={12} className={payosStatus === 'waiting' ? 'animate-spin' : ''} />
              Hết hạn sau: {formatCountdown(timeLeft)}
            </span>
          </div>

          <p className="text-xs font-semibold text-slate-600 dark:text-slate-300">
            Mở ứng dụng ngân hàng và quét mã VietQR để hoàn tất thanh toán.
          </p>

          <button
            type="button"
            onClick={handleCancelQr}
            disabled={payosLoading}
            className="mx-auto inline-flex items-center gap-1 text-xs font-bold text-rose-500 transition hover:text-rose-700 disabled:opacity-50"
          >
            <XCircle size={14} /> Hủy mã / Đổi voucher
          </button>
        </div>
      )}

      {payosStatus === 'success' && (
        <div className="space-y-2 rounded-2xl border border-emerald-200/80 bg-emerald-50/70 py-6 text-center dark:border-emerald-800/40 dark:bg-emerald-950/30">
          <CheckCircle2
            size={40}
            className="mx-auto text-emerald-600 dark:text-emerald-400"
          />
          <h4 className="text-base font-black text-emerald-700 dark:text-emerald-300">
            Thanh toán thành công!
          </h4>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Cảm ơn quý khách đã sử dụng dịch vụ.
          </p>
        </div>
      )}

      {/* Optional fallback to call staff / cash */}
      {payosStatus !== 'waiting' && payosStatus !== 'success' && !isAwaitingStaffPayment && (
        <button
          type="button"
          onClick={onRequestStaffPayment}
          disabled={requestingStaff || (unfinishedItems && !canCreateQr)}
          className="flex w-full items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white py-3 text-xs font-bold text-slate-600 transition hover:bg-slate-50 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
        >
          {requestingStaff ? (
            <Loader2 size={15} className="animate-spin" />
          ) : (
            <CreditCard size={15} />
          )}
          Gọi nhân viên hỗ trợ / Thanh toán tiền mặt
        </button>
      )}
    </div>
  );
};

const CurrentOrderSheetContent = ({
  isOpen,
  order,
  activePromotion = null,
  onClose,
  onRequestPayment,
  onPaymentSuccess,
  tableStatus,
  tableCode,
  sessionToken,
  restaurantSettings,
}) => {
  const [requesting, setRequesting] = useState(false);
  const [justRequested, setJustRequested] = useState(false);

  const selfCheckoutAvailable = isCustomerSelfCheckoutAvailable(restaurantSettings);
  const checkout = useCustomerSelfCheckout({
    tableCode,
    sessionToken,
    order,
    activePromotion,
    restaurantSettings,
    onPaymentSuccess,
  });

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
          {checkout.summary.discountAmount > 0 && (
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
            </div>
          )}
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
              {checkout.summary.discountAmount > 0 ? 'Tổng thanh toán' : 'Tạm tính'}
            </span>
            <span className="font-mono text-2xl font-black tracking-tight text-indigo-600 dark:text-indigo-400">
              {fmtVND(checkout.summary.finalAmount)}
            </span>
          </div>

          {items.length > 0 && order.status !== 'COMPLETED' && order.status !== 'CANCELLED' && (
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
