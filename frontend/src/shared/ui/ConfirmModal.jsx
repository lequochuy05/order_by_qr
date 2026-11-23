import { AlertTriangle, Check, Loader2, Trash2, X } from 'lucide-react';
import { useTranslation } from '@shared/lib/i18n';
import SharedModal from './SharedModal.jsx';

const DELETE_TITLE_PATTERN = /(xóa|xoá|delete|remove)/i;
const DANGER_PATTERN = /(xóa|xoá|hủy|huỷ|delete|remove|danger|warning)/i;

const ConfirmModal = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText,
  cancelText,
  variant,
  loading = false,
}) => {
  const { isEn } = useTranslation('common');
  const resolvedTitle = title || (isEn ? 'Confirmation' : 'Xác nhận');
  const isDeleteAction = DELETE_TITLE_PATTERN.test(resolvedTitle);
  const isDanger = variant
    ? variant === 'danger'
    : DANGER_PATTERN.test(`${resolvedTitle} ${message || ''}`);

  const resolvedConfirmText =
    confirmText ||
    (isDeleteAction ? (isEn ? 'Delete' : 'Xóa ngay') : isEn ? 'Confirm' : 'Xác nhận');
  const resolvedCancelText = cancelText || (isEn ? 'Cancel' : 'Hủy bỏ');
  const loadingText = isEn ? 'Processing...' : 'Đang xử lý...';
  const badgeLabel = isDeleteAction
    ? isEn
      ? 'Irreversible action'
      : 'Thao tác không thể hoàn tác'
    : isDanger
      ? isEn
        ? 'Action confirmation'
        : 'Xác nhận thao tác'
      : isEn
        ? 'Please confirm'
        : 'Xác nhận thực hiện';

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={loading ? undefined : onClose}
      ariaLabel={resolvedTitle}
      className="max-w-md !p-0"
      backdropClassName="bg-slate-950/60 backdrop-blur-md"
    >
      <div
        className={`h-1.5 w-full ${
          isDanger
            ? 'bg-gradient-to-r from-rose-500 via-red-500 to-orange-500'
            : 'bg-gradient-to-r from-indigo-500 via-violet-500 to-amber-500'
        }`}
      />

      <div className="relative px-6 pb-6 pt-7 sm:px-8 sm:pb-7">
        <button
          type="button"
          onClick={onClose}
          disabled={loading}
          aria-label={isEn ? 'Close' : 'Đóng'}
          className="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-xl border border-slate-200/80 bg-slate-50/80 text-slate-400 transition-all hover:border-slate-300 hover:bg-slate-100 hover:text-slate-700 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-800 dark:bg-slate-800/80 dark:text-slate-400 dark:hover:bg-slate-700 dark:hover:text-slate-200"
        >
          <X size={16} />
        </button>

        <div className="flex flex-col items-center text-center">
          <div
            className={`mb-4 flex h-16 w-16 items-center justify-center rounded-2xl border ring-8 ${
              isDanger
                ? 'border-rose-200/70 bg-rose-50 text-rose-600 ring-rose-500/5 dark:border-rose-500/20 dark:bg-rose-500/10 dark:text-rose-400 dark:ring-rose-500/5'
                : 'border-amber-200/70 bg-amber-50 text-amber-600 ring-amber-500/5 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-400 dark:ring-amber-500/5'
            }`}
          >
            {isDeleteAction ? <Trash2 size={28} strokeWidth={2.2} /> : <AlertTriangle size={28} strokeWidth={2.2} />}
          </div>

          <span
            className={`mb-2 inline-flex items-center rounded-full px-3 py-0.5 text-[10px] font-extrabold uppercase tracking-[0.16em] ${
              isDanger
                ? 'bg-rose-50 text-rose-600 dark:bg-rose-500/10 dark:text-rose-400'
                : 'bg-amber-50 text-amber-700 dark:bg-amber-500/10 dark:text-amber-300'
            }`}
          >
            {badgeLabel}
          </span>

          <h3 className="text-xl font-black tracking-tight text-slate-900 dark:text-white">
            {resolvedTitle}
          </h3>

          {message && (
            <div className="mt-4 w-full rounded-2xl border border-slate-200/70 bg-slate-50/75 px-4 py-3.5 text-sm leading-relaxed whitespace-pre-line text-slate-600 dark:border-slate-800 dark:bg-slate-800/50 dark:text-slate-300">
              {message}
            </div>
          )}
        </div>

        <div className="mt-6 flex items-center gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="inline-flex h-11 flex-1 items-center justify-center rounded-xl border border-slate-200/90 bg-white px-4 text-sm font-bold text-slate-600 shadow-xs transition-all hover:border-slate-300 hover:bg-slate-100/80 hover:text-slate-900 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-700/80 dark:bg-slate-800/80 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white"
          >
            {resolvedCancelText}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={loading}
            className={`inline-flex h-11 flex-1 items-center justify-center gap-2 rounded-xl px-4 text-sm font-bold text-white shadow-md transition-all active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60 ${
              isDanger
                ? 'bg-gradient-to-r from-rose-600 to-red-600 shadow-rose-600/25 hover:from-rose-500 hover:to-red-500 hover:shadow-lg hover:shadow-rose-600/30'
                : 'bg-gradient-to-r from-indigo-600 to-violet-600 shadow-indigo-600/25 hover:from-indigo-500 hover:to-violet-500 hover:shadow-lg hover:shadow-indigo-600/30'
            }`}
          >
            {loading ? (
              <Loader2 size={16} className="animate-spin" />
            ) : isDeleteAction ? (
              <Trash2 size={16} />
            ) : (
              <Check size={16} />
            )}
            <span>{loading ? loadingText : resolvedConfirmText}</span>
          </button>
        </div>
      </div>
    </SharedModal>
  );
};

export default ConfirmModal;
