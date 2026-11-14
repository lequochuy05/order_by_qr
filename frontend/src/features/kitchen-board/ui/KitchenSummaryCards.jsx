import { createElement } from 'react';
import { AlertTriangle, CheckCircle2, Clock3, Flame } from 'lucide-react';

const TONES = {
  orange: {
    gradient: 'from-amber-500 to-orange-500 shadow-amber-500/25',
    text: 'text-amber-600 dark:text-amber-400',
    border: 'border-amber-200/60 dark:border-amber-800/40',
  },
  blue: {
    gradient: 'from-indigo-600 to-violet-500 shadow-indigo-500/25',
    text: 'text-indigo-600 dark:text-indigo-400',
    border: 'border-indigo-200/60 dark:border-indigo-800/40',
  },
  green: {
    gradient: 'from-emerald-500 to-teal-500 shadow-emerald-500/25',
    text: 'text-emerald-600 dark:text-emerald-400',
    border: 'border-emerald-200/60 dark:border-emerald-800/40',
  },
  red: {
    gradient: 'from-rose-500 to-red-500 shadow-rose-500/25',
    text: 'text-rose-600 dark:text-rose-400',
    border: 'border-rose-200/60 dark:border-rose-800/40',
  },
  slate: {
    gradient: 'from-slate-600 to-slate-700 shadow-slate-500/25',
    text: 'text-slate-600 dark:text-slate-400',
    border: 'border-slate-200/60 dark:border-slate-800/40',
  },
};

const SummaryCard = ({ label, value, icon, tone }) => {
  const currentTone = TONES[tone] || TONES.slate;

  return (
    <div className="group rounded-3xl border border-slate-200/80 bg-white p-5 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-xl dark:border-slate-800/80 dark:bg-slate-900/90">
      <div className="flex items-start justify-between gap-2">
        <div className="space-y-1">
          <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">{label}</p>
          <p className="text-2xl sm:text-3xl font-black font-mono tracking-tight text-slate-900 dark:text-white">{value}</p>
        </div>
        <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr ${currentTone.gradient} text-white shadow-lg group-hover:scale-110 transition-transform duration-300`}>
          {createElement(icon, { size: 20 })}
        </div>
      </div>
    </div>
  );
};

const KitchenSummaryCards = ({ summary, overdueMinutes }) => (
  <div className="mt-6 grid grid-cols-2 gap-3 lg:grid-cols-5 sm:gap-4">
    <SummaryCard label="Chờ nấu" value={summary.pending} icon={Clock3} tone="orange" />
    <SummaryCard label="Đang nấu" value={summary.cooking} icon={Flame} tone="blue" />
    <SummaryCard label="Vừa hoàn thành" value={summary.finished} icon={CheckCircle2} tone="green" />
    <SummaryCard
      label={`Quá ${overdueMinutes} phút`}
      value={summary.overdue}
      icon={AlertTriangle}
      tone="red"
    />
    <SummaryCard
      label="Chờ trung bình"
      value={`${summary.averageWait}p`}
      icon={Clock3}
      tone="slate"
    />
  </div>
);

export default KitchenSummaryCards;
