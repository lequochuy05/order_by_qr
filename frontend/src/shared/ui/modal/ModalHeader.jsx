import { X } from 'lucide-react';

const ModalHeader = ({ title, subtitle, onClose, disabled = false, className = '' }) => (
  <div
    className={`flex shrink-0 items-center justify-between border-b border-slate-100 bg-white px-6 py-5 transition-colors dark:border-slate-800 dark:bg-slate-900 sm:px-8 sm:py-6 ${className}`}
  >
    <div className="min-w-0">
      <h2 className="truncate text-xl font-black tracking-tight text-slate-900 dark:text-slate-100">
        {title}
      </h2>
      {subtitle && (
        <p className="mt-0.5 truncate text-[10px] font-bold uppercase tracking-widest text-slate-400 dark:text-slate-500">
          {subtitle}
        </p>
      )}
    </div>
    <button
      type="button"
      onClick={onClose}
      disabled={disabled}
      aria-label="Đóng"
      className="rounded-full p-2.5 text-slate-400 transition-all hover:bg-slate-100 hover:text-slate-600 active:scale-90 disabled:cursor-not-allowed disabled:opacity-50 dark:hover:bg-slate-800 dark:hover:text-slate-200"
    >
      <X size={20} />
    </button>
  </div>
);

export default ModalHeader;
