import { useEffect, useState } from 'react';
import {
  CheckCircle2,
  Clock,
  ExternalLink,
  Loader2,
  QrCode,
  RefreshCw,
  XCircle,
} from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';

import {
  formatBillingCycleLabel,
  useCancelSubscriptionInvoiceMutation,
  useSyncSubscriptionInvoiceMutation,
} from '@entities/subscription';
import { fmtVND } from '@shared/lib/formatters.js';
import { useTranslation } from '@shared/lib/i18n';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import { ModalHeader, SharedModal } from '@shared/ui';

const formatCountdown = (seconds) => {
  const safe = Math.max(0, Number(seconds || 0));
  const mins = Math.floor(safe / 60);
  const secs = safe % 60;
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
};

const computeRemainingSeconds = (expiresAt) => {
  if (!expiresAt) return 0;
  const diff = Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000);
  return Math.max(0, diff);
};

const SubscriptionQrModal = ({ invoice, onClose, onInvoiceUpdated }) => {
  const { language, isEn } = useTranslation();
  const [, setTick] = useState(0);

  const { mutateAsync: syncInvoice, isPending: isSyncing } =
    useSyncSubscriptionInvoiceMutation();
  const { mutateAsync: cancelInvoice, isPending: isCancelling } =
    useCancelSubscriptionInvoiceMutation();

  const busy = isSyncing || isCancelling;

  useEffect(() => {
    if (!invoice?.expiresAt || invoice?.status !== 'PENDING') return undefined;
    const timer = window.setInterval(() => {
      setTick((value) => value + 1);
    }, 1000);
    return () => window.clearInterval(timer);
  }, [invoice?.expiresAt, invoice?.status]);

  if (!invoice) return null;

  const remainingSeconds = computeRemainingSeconds(invoice.expiresAt);

  const isPaid = invoice.status === 'PAID';
  const isExpired = invoice.status === 'EXPIRED' || (invoice.status === 'PENDING' && remainingSeconds <= 0);

  const handleSync = async () => {
    if (busy) return;
    try {
      const updated = await syncInvoice(invoice.id);
      onInvoiceUpdated?.(updated);
      if (updated?.status === 'PAID') {
        showSuccessToast(
          isEn
            ? 'Payment confirmed! QROS Pro is now active.'
            : 'Đã xác nhận thanh toán! Gói QROS Pro đã được mở khóa.',
        );
        onClose?.();
      } else if (updated?.status === 'PENDING') {
        showSuccessToast(
          isEn
            ? 'Payment not received yet. Please scan the VietQR code and retry.'
            : 'Chưa nhận được chuyển khoản. Vui lòng quét mã VietQR rồi kiểm tra lại.',
        );
      }
    } catch (error) {
      showErrorToast(error);
    }
  };

  const handleCancel = async () => {
    if (busy) return;
    try {
      const updated = await cancelInvoice(invoice.id);
      onInvoiceUpdated?.(updated);
      showSuccessToast(
        isEn ? 'Pending upgrade invoice cancelled.' : 'Đã hủy giao dịch nâng cấp đang chờ.',
      );
      onClose?.();
    } catch (error) {
      showErrorToast(error);
    }
  };

  return (
    <SharedModal
      isOpen
      onClose={() => {
        if (!busy) onClose?.();
      }}
      className="max-w-lg !p-0"
      ariaLabel={isEn ? 'PayOS VietQR Upgrade Payment' : 'Thanh toán nâng cấp Pro qua VietQR'}
    >
      <ModalHeader
        title={isEn ? 'Scan VietQR to Upgrade Pro' : 'Quét mã VietQR nâng cấp gói Pro'}
        subtitle={`${invoice.invoiceCode} · ${formatBillingCycleLabel(invoice.billingCycle, language)}`}
        onClose={() => {
          if (!busy) onClose?.();
        }}
        disabled={busy}
      />

      <div className="space-y-5 p-6">
        <div className="flex items-center justify-between rounded-2xl border border-indigo-200/80 bg-indigo-50/70 px-4 py-3 dark:border-indigo-500/25 dark:bg-indigo-950/35">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-300">
              {isEn ? 'Amount due' : 'Số tiền thanh toán'}
            </p>
            <p className="font-mono text-2xl font-black tabular-nums text-slate-900 dark:text-white">
              {fmtVND(invoice.amount)}
            </p>
          </div>
          {invoice.status === 'PENDING' && (
            <div
              className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 font-mono text-xs font-bold tabular-nums ${
                isExpired
                  ? 'bg-rose-100 text-rose-700 dark:bg-rose-950/50 dark:text-rose-300'
                  : 'bg-white text-slate-700 shadow-xs dark:bg-slate-900 dark:text-slate-200'
              }`}
            >
              <Clock size={14} />
              <span>{isExpired ? (isEn ? 'Expired' : 'Hết hạn') : formatCountdown(remainingSeconds)}</span>
            </div>
          )}
        </div>

        <div className="flex flex-col items-center justify-center rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-950">
          {isPaid ? (
            <div className="flex flex-col items-center gap-2 py-6 text-emerald-600 dark:text-emerald-400">
              <CheckCircle2 size={48} />
              <p className="text-sm font-bold">
                {isEn ? 'Payment Confirmed!' : 'Thanh toán thành công!'}
              </p>
            </div>
          ) : invoice.qrCode ? (
            <>
              <div className="rounded-2xl border border-slate-200 bg-white p-3 shadow-xs">
                <QRCodeSVG value={invoice.qrCode} size={220} level="H" includeMargin />
              </div>
              <p className="mt-3 text-center text-xs text-slate-500 dark:text-slate-400">
                {isEn
                  ? 'Open your banking app to scan this VietQR code. Pro unlocks automatically upon transfer.'
                  : 'Mở ứng dụng ngân hàng để quét mã VietQR. Gói Pro tự động mở khóa ngay sau khi chuyển khoản.'}
              </p>
            </>
          ) : (
            <div className="flex flex-col items-center gap-2 py-8 text-slate-500 dark:text-slate-400">
              <QrCode size={40} />
              <p className="text-xs font-medium">
                {isEn
                  ? 'Use the PayOS checkout link below to complete payment.'
                  : 'Mở liên kết thanh toán PayOS bên dưới để hoàn tất.'}
              </p>
            </div>
          )}

          {invoice.checkoutUrl && !isPaid && (
            <a
              href={invoice.checkoutUrl}
              target="_blank"
              rel="noreferrer"
              className="mt-3 inline-flex items-center gap-1.5 text-xs font-bold text-indigo-600 hover:underline dark:text-indigo-400"
            >
              <span>{isEn ? 'Open PayOS Checkout Page' : 'Mở trang thanh toán PayOS'}</span>
              <ExternalLink size={13} />
            </a>
          )}
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2.5 border-t border-slate-200/80 bg-slate-50/80 px-6 py-4 dark:border-slate-800 dark:bg-slate-900/80">
        {invoice.status === 'PENDING' ? (
          <button
            type="button"
            onClick={handleCancel}
            disabled={busy}
            className="inline-flex h-10 items-center gap-1.5 rounded-xl border border-rose-200 bg-rose-50 px-3.5 text-xs font-bold text-rose-700 transition hover:bg-rose-600 hover:text-white disabled:opacity-50 dark:border-rose-800/50 dark:bg-rose-950/40 dark:text-rose-300"
          >
            {isCancelling ? <Loader2 size={14} className="animate-spin" /> : <XCircle size={14} />}
            <span>{isEn ? 'Cancel Invoice' : 'Hủy giao dịch'}</span>
          </button>
        ) : (
          <div />
        )}

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onClose}
            disabled={busy}
            className="inline-flex h-10 items-center justify-center rounded-xl border border-slate-200 bg-white px-4 text-xs font-bold text-slate-700 transition hover:bg-slate-100 disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
          >
            {isEn ? 'Close' : 'Đóng'}
          </button>

          {invoice.status === 'PENDING' && (
            <button
              type="button"
              onClick={handleSync}
              disabled={busy}
              className="inline-flex h-10 items-center gap-2 rounded-xl bg-indigo-600 px-4 text-xs font-bold text-white shadow-sm transition hover:bg-indigo-700 disabled:opacity-50"
            >
              {isSyncing ? <Loader2 size={14} className="animate-spin" /> : <RefreshCw size={14} />}
              <span>{isEn ? 'Check Payment Status' : 'Kiểm tra thanh toán'}</span>
            </button>
          )}
        </div>
      </div>
    </SharedModal>
  );
};

export default SubscriptionQrModal;
