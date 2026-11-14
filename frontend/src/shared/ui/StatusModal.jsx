import { CheckCircle2, AlertCircle } from 'lucide-react';
import SharedModal from './SharedModal.jsx';

const StatusModal = ({ isOpen, onClose, type = 'success', title, message }) => {
  const isSuccess = type === 'success';

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      ariaLabel={title || (isSuccess ? 'Thành công' : 'Thất bại')}
      className="max-w-sm overflow-hidden p-0 sm:p-0 rounded-3xl border border-slate-200/80 dark:border-slate-800/80 dark:bg-slate-900"
      backdropClassName="bg-black/50 backdrop-blur-sm"
    >
      {/* Header Icon */}
      <div
        className={`p-6 flex flex-col items-center justify-center text-center border-b ${
          isSuccess
            ? 'bg-emerald-50/80 dark:bg-emerald-500/10 border-emerald-100 dark:border-emerald-500/20'
            : 'bg-rose-50/80 dark:bg-rose-500/10 border-rose-100 dark:border-rose-500/20'
        }`}
      >
        <div
          className={`p-3.5 rounded-2xl mb-3 text-white shadow-lg ${
            isSuccess
              ? 'bg-gradient-to-tr from-emerald-500 to-teal-400 shadow-emerald-500/25'
              : 'bg-gradient-to-tr from-rose-500 to-red-400 shadow-rose-500/25'
          }`}
        >
          {isSuccess ? <CheckCircle2 size={32} /> : <AlertCircle size={32} />}
        </div>
        <h3 className="text-xl font-bold text-slate-900 dark:text-white">
          {title || (isSuccess ? 'Thành công' : 'Thất bại')}
        </h3>
      </div>

      {/* Content */}
      <div className="p-6 text-center">
        <p className="text-slate-600 text-sm mb-6 leading-relaxed whitespace-pre-line dark:text-slate-300">
          {message}
        </p>

        <button
          onClick={onClose}
          className={`w-full py-3 rounded-xl font-bold text-white transition-all active:scale-95 shadow-md ${
            isSuccess
              ? 'bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 shadow-emerald-600/25'
              : 'bg-gradient-to-r from-rose-600 to-red-600 hover:from-rose-500 hover:to-red-500 shadow-rose-600/25'
          }`}
        >
          Đóng
        </button>
      </div>
    </SharedModal>
  );
};

export default StatusModal;
