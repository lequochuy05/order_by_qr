import { BellRing } from 'lucide-react';

const CallStaffButton = ({ openCount = 0, onClick, disabled = false }) => (
  <button
    type="button"
    onClick={onClick}
    disabled={disabled}
    aria-label="Gọi nhân viên hỗ trợ"
    className={`relative inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-bold shadow-sm transition-all active:scale-95 disabled:cursor-not-allowed disabled:opacity-50 ${
      openCount > 0
        ? 'border-amber-400/50 bg-amber-500/20 text-amber-200 hover:bg-amber-500/30'
        : 'border-white/20 bg-white/10 text-white hover:bg-white/20'
    }`}
  >
    <BellRing size={13} className={openCount > 0 ? 'animate-bounce' : ''} />
    <span>Gọi NV</span>
    {openCount > 0 && (
      <span className="inline-flex h-4 min-w-4 items-center justify-center rounded-full bg-amber-500 px-1 text-[10px] font-black text-slate-950">
        {openCount}
      </span>
    )}
  </button>
);

export default CallStaffButton;
