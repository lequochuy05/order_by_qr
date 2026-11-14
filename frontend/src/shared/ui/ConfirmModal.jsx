import { AlertTriangle } from 'lucide-react';
import SharedModal from './SharedModal.jsx';

const ConfirmModal = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText = 'Xác nhận',
  cancelText = 'Hủy',
  loading = false,
}) => {
  return (
    <SharedModal
      isOpen={isOpen}
      onClose={loading ? undefined : onClose}
      ariaLabel={title || 'Xác nhận'}
      className="max-w-sm overflow-hidden p-0 sm:p-0 rounded-3xl border border-slate-200/80 dark:border-slate-800/80 dark:bg-slate-900"
      backdropClassName="bg-black/50 backdrop-blur-sm"
    >
      {/* Header Icon */}
      <div className="p-6 flex flex-col items-center justify-center text-center bg-amber-50/80 dark:bg-amber-500/10 border-b border-amber-100 dark:border-amber-500/20">
        <div className="p-3.5 rounded-2xl mb-3 bg-gradient-to-tr from-amber-500 to-yellow-400 text-white shadow-lg shadow-amber-500/25">
          <AlertTriangle size={32} />
        </div>
        <h3 className="text-xl font-bold text-slate-900 dark:text-white">
          {title || 'Xác nhận'}
        </h3>
      </div>

      {/* Content */}
      <div className="p-6 text-center">
        <p className="text-slate-600 text-sm mb-6 leading-relaxed whitespace-pre-line dark:text-slate-300">
          {message}
        </p>

        <div className="flex gap-3">
          <button
            onClick={onClose}
            disabled={loading}
            className="flex-1 py-3 rounded-xl font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-all active:scale-95 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
          >
            {cancelText}
          </button>
          <button
            onClick={onConfirm}
            disabled={loading}
            className="flex-1 py-3 rounded-xl font-bold text-white bg-gradient-to-r from-rose-600 to-red-600 hover:from-rose-500 hover:to-red-500 transition-all active:scale-95 shadow-md shadow-rose-500/25 disabled:opacity-60"
          >
            {loading ? 'Đang xử lý...' : confirmText}
          </button>
        </div>
      </div>
    </SharedModal>
  );
};

export default ConfirmModal;
