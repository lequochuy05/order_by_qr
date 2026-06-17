import { AlertTriangle, X } from 'lucide-react';

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
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/40 z-[70] flex items-center justify-center p-4 backdrop-blur-[2px] animate-in fade-in duration-200">
      <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200 dark:bg-slate-900">
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
      </div>
    </div>
  );
};

export default ConfirmModal;
