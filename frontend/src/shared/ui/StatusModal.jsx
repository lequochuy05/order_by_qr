import { AlertCircle, CheckCircle2, X } from 'lucide-react';
import SharedModal from './SharedModal.jsx';

const StatusModal = ({ isOpen, onClose, type = 'success', title, message }) => {
  const isSuccess = type === 'success';
  const resolvedTitle = title || (isSuccess ? 'Thành công' : 'Thất bại');

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      ariaLabel={resolvedTitle}
      className="max-w-md !p-0"
      backdropClassName="bg-slate-950/60 backdrop-blur-md"
    >
      <div
        className={`h-1.5 w-full ${
          isSuccess
            ? 'bg-gradient-to-r from-emerald-500 via-teal-500 to-cyan-500'
            : 'bg-gradient-to-r from-rose-500 via-red-500 to-orange-500'
        }`}
      />

      <div className="relative px-6 pb-6 pt-7 sm:px-8 sm:pb-7">
        <button
          type="button"
          onClick={onClose}
          aria-label="Đóng"
          className="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-xl border border-slate-200/80 bg-slate-50/80 text-slate-400 transition-all hover:border-slate-300 hover:bg-slate-100 hover:text-slate-700 active:scale-95 dark:border-slate-800 dark:bg-slate-800/80 dark:text-slate-400 dark:hover:bg-slate-700 dark:hover:text-slate-200"
        >
          <X size={16} />
        </button>

        <div className="flex flex-col items-center text-center">
          <div
            className={`mb-4 flex h-16 w-16 items-center justify-center rounded-2xl border ring-8 ${
              isSuccess
                ? 'border-emerald-200/70 bg-emerald-50 text-emerald-600 ring-emerald-500/5 dark:border-emerald-500/20 dark:bg-emerald-500/10 dark:text-emerald-400 dark:ring-emerald-500/5'
                : 'border-rose-200/70 bg-rose-50 text-rose-600 ring-rose-500/5 dark:border-rose-500/20 dark:bg-rose-500/10 dark:text-rose-400 dark:ring-rose-500/5'
            }`}
          >
            {isSuccess ? (
              <CheckCircle2 size={28} strokeWidth={2.2} />
            ) : (
              <AlertCircle size={28} strokeWidth={2.2} />
            )}
          </div>

          <h3 className="text-xl font-black tracking-tight text-slate-900 dark:text-white">
            {resolvedTitle}
          </h3>

          {message && (
            <div className="mt-4 w-full rounded-2xl border border-slate-200/70 bg-slate-50/75 px-4 py-3.5 text-sm leading-relaxed whitespace-pre-line text-slate-600 dark:border-slate-800 dark:bg-slate-800/50 dark:text-slate-300">
              {message}
            </div>
          )}
        </div>

        <div className="mt-6">
          <button
            type="button"
            onClick={onClose}
            className={`inline-flex h-11 w-full items-center justify-center rounded-xl px-5 text-sm font-bold text-white shadow-md transition-all active:scale-[0.98] ${
              isSuccess
                ? 'bg-gradient-to-r from-emerald-600 to-teal-600 shadow-emerald-600/25 hover:from-emerald-500 hover:to-teal-500 hover:shadow-lg hover:shadow-emerald-600/30'
                : 'bg-gradient-to-r from-rose-600 to-red-600 shadow-rose-600/25 hover:from-rose-500 hover:to-red-500 hover:shadow-lg hover:shadow-rose-600/30'
            }`}
          >
            Đóng
          </button>
        </div>
      </div>
    </SharedModal>
  );
};

export default StatusModal;
