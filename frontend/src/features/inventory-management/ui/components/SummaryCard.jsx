const TONE_CLASSES = {
  orange: 'bg-orange-50 text-orange-600 dark:bg-orange-500/10 dark:text-orange-300',
  green: 'bg-emerald-50 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300',
  amber: 'bg-amber-50 text-amber-600 dark:bg-amber-500/10 dark:text-amber-300',
  red: 'bg-red-50 text-red-600 dark:bg-red-500/10 dark:text-red-300',
};

const SummaryCard = ({ icon, label, value, tone = 'orange' }) => (
  <div className="min-w-0 rounded-3xl border border-slate-100 bg-white p-4 shadow-sm transition-colors dark:border-slate-800 dark:bg-slate-900 sm:p-5">
    <div className="flex items-center gap-4">
      <div className={`rounded-2xl p-3 ${TONE_CLASSES[tone]}`}>
        {createElement(icon, { size: 22 })}
      </div>
      <div>
        <p className="text-[10px] font-black uppercase tracking-[0.16em] text-slate-400 dark:text-slate-500">{label}</p>
        <p className="text-3xl font-black text-slate-800 dark:text-slate-100">{value}</p>
      </div>
    </div>
  </div>
);

export default SummaryCard;
import { createElement } from 'react';
