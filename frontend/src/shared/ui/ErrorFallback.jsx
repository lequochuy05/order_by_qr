import { AlertTriangle, RefreshCcw } from 'lucide-react';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';

const ErrorFallback = ({ error, resetErrorBoundary, fullScreen = false }) => {
  const message = error
    ? buildErrorMessage(error, { includeDetails: false })
    : 'Ứng dụng gặp sự cố không mong muốn. Vui lòng thử lại.';

  return (
    <div
      role="alert"
      className={`flex items-center justify-center bg-slate-50 px-4 py-10 dark:bg-slate-950 ${
        fullScreen ? 'min-h-screen' : 'min-h-64 rounded-2xl'
      }`}
    >
      <div className="w-full max-w-lg rounded-3xl border border-red-100 bg-white p-8 text-center shadow-xl shadow-red-100/40 dark:border-red-500/20 dark:bg-slate-900 dark:shadow-none">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-red-50 text-red-500 dark:bg-red-500/10 dark:text-red-400">
          <AlertTriangle size={30} aria-hidden="true" />
        </div>

        <h1 className="mt-5 text-2xl font-black text-slate-900 dark:text-white">Đã xảy ra lỗi</h1>
        <p className="mt-3 break-words text-sm leading-6 text-slate-600 dark:text-slate-300">
          {message}
        </p>

        <button
          type="button"
          onClick={resetErrorBoundary}
          className="mt-6 inline-flex items-center justify-center gap-2 rounded-xl bg-orange-500 px-5 py-3 text-sm font-bold text-white shadow-lg shadow-orange-200 transition hover:bg-orange-600 active:scale-95 dark:shadow-none"
        >
          <RefreshCcw size={17} aria-hidden="true" />
          Thử lại
        </button>
      </div>
    </div>
  );
};

export default ErrorFallback;
