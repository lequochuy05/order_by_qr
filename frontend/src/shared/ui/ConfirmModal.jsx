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
      className="max-w-sm overflow-hidden p-0 sm:p-0 dark:bg-slate-900"
      backdropClassName="bg-black/40 backdrop-blur-[2px]"
    >
      {/* Header Icon */}
      <div className="p-6 flex flex-col items-center justify-center text-center bg-amber-50 dark:bg-amber-500/10">
        <div className="p-3 rounded-full mb-3 bg-amber-100 text-amber-600 dark:bg-amber-500/15 dark:text-amber-300">
          <AlertTriangle size={48} />
        </div>
        <h3 className="text-xl font-bold text-amber-700 dark:text-amber-300">
          {title || 'Xác nhận'}
        </h3>
      </div>

      {/* Content */}
      <div className="p-6 text-center">
        <p className="text-gray-600 text-sm mb-6 leading-relaxed whitespace-pre-line dark:text-slate-300">
          {message}
        </p>

        <div className="flex gap-3">
          <button
            onClick={onClose}
            disabled={loading}
            className="flex-1 py-3 rounded-xl font-bold text-gray-600 bg-gray-100 hover:bg-gray-200 transition-all active:scale-95 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700"
          >
            {cancelText}
          </button>
          <button
            onClick={onConfirm}
            disabled={loading}
            className="flex-1 py-3 rounded-xl font-bold text-white bg-red-500 hover:bg-red-600 transition-all active:scale-95 shadow-md shadow-red-200 disabled:opacity-60"
          >
            {loading ? 'Đang xử lý...' : confirmText}
          </button>
        </div>
      </div>
    </SharedModal>
  );
};

export default ConfirmModal;
