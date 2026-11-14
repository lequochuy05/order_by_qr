import { Send } from 'lucide-react';

const SubmitOrderButton = ({ onClick, disabled = false, loading = false, totalLabel = '' }) => (
  <button
    type="button"
    onClick={onClick}
    disabled={disabled || loading}
    className="flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 px-5 py-4 text-xs font-bold uppercase tracking-wider text-white shadow-lg shadow-indigo-600/25 transition-all active:scale-[0.98] disabled:cursor-not-allowed disabled:bg-slate-300 dark:disabled:bg-slate-800 disabled:shadow-none"
  >
    <Send size={16} />
    <span>{loading ? 'Đang gửi...' : `Đặt món${totalLabel ? ` - ${totalLabel}` : ''}`}</span>
  </button>
);

export default SubmitOrderButton;
