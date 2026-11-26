import React from 'react';
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  CreditCard,
  Loader2,
  QrCode,
  RefreshCw,
  Tag,
  X,
  XCircle,
} from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';

import { fmtVND } from '@shared/lib/formatters.js';
import { PayosCountdown } from '@entities/payment';

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
    handleVoucherInputChange,
    handleApplyVoucher,
    handleClearVoucher,
    handleCreateQr,
    handleCancelQr,
    handlePayosExpired,
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
        {summary.serviceChargeAmount > 0 && (
          <div className="flex justify-between gap-2 text-slate-500 dark:text-slate-400">
            <span>
              Phí dịch vụ
              {summary.serviceChargePercent > 0 ? ` (${summary.serviceChargePercent}%)` : ''}:
            </span>
            <span className="shrink-0 font-mono font-bold text-slate-700 dark:text-slate-200">
              +{fmtVND(summary.serviceChargeAmount)}
            </span>
          </div>
        )}
        {summary.taxAmount > 0 && (
          <div className="flex justify-between gap-2 text-slate-500 dark:text-slate-400">
            <span>
              Thuế VAT{summary.taxPercent > 0 ? ` (${summary.taxPercent}%)` : ''}:
            </span>
            <span className="shrink-0 font-mono font-bold text-slate-700 dark:text-slate-200">
              +{fmtVND(summary.taxAmount)}
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
            {summary.finalAmount === 0 ? 'Xác nhận thanh toán 0đ' : 'Tạo mã VietQR thanh toán'}
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
            <PayosCountdown
              expiresAt={payosData.expiresAt}
              active={payosStatus === 'waiting'}
              onExpire={handlePayosExpired}
            />
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

export default SelfCheckoutPanel;
