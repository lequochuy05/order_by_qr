import { X } from 'lucide-react';

const ModalHeader = ({ title, subtitle, onClose, disabled = false, className = '' }) => (
  <div
    className={`flex shrink-0 items-center justify-between border-b bg-white px-6 py-5 sm:px-8 sm:py-6 ${className}`}
  >
    <div className="min-w-0">
      <h2 className="truncate text-xl font-black tracking-tight text-gray-800">{title}</h2>
      {subtitle && (
        <p className="mt-0.5 truncate text-[10px] font-bold uppercase tracking-widest text-gray-400">
          {subtitle}
        </p>
      )}
    </div>
    <button
      type="button"
      onClick={onClose}
      disabled={disabled}
      aria-label="Đóng"
      className="rounded-full p-2.5 text-gray-400 transition-all hover:bg-gray-100 active:scale-90 disabled:cursor-not-allowed disabled:opacity-50"
    >
      <X size={20} />
    </button>
  </div>
);

export default ModalHeader;
