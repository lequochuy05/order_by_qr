import { createElement } from 'react';

const TONE_CONFIG = {
  orange: {
    badge: 'bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-lg shadow-indigo-500/25',
    text: 'text-indigo-600 dark:text-indigo-400',
  },
  green: {
    badge: 'bg-gradient-to-tr from-emerald-500 to-teal-500 text-white shadow-lg shadow-emerald-500/25',
    text: 'text-emerald-600 dark:text-emerald-400',
  },
  amber: {
    badge: 'bg-gradient-to-tr from-amber-500 to-orange-500 text-white shadow-lg shadow-amber-500/25',
    text: 'text-amber-600 dark:text-amber-400',
  },
  red: {
    badge: 'bg-gradient-to-tr from-rose-500 to-red-500 text-white shadow-lg shadow-rose-500/25',
    text: 'text-rose-600 dark:text-rose-400',
  },
};

const SummaryCard = ({ icon, label, value, tone = 'orange' }) => {
  const conf = TONE_CONFIG[tone] || TONE_CONFIG.orange;

  return (
    <div className="min-w-0 rounded-3xl border border-[#1E293B] bg-white/90 p-5 shadow-sm backdrop-blur-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-md dark:border-[#1E293B] dark:bg-[#0F172A]/90">
      <div className="flex items-center gap-4">
        <div className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl ${conf.badge}`}>
          {createElement(icon, { size: 22 })}
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
            {label}
          </p>
          <p className="text-3xl font-black font-mono tracking-tight text-slate-900 dark:text-white">
            {value}
          </p>
        </div>
      </div>
    </div>
  );
};

export default SummaryCard;
