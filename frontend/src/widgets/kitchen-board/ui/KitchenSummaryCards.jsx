import { createElement } from 'react';
import { AlertTriangle, CheckCircle2, Clock3, Flame } from 'lucide-react';

const TONES = {
  orange:
    'bg-orange-50 text-orange-600 ring-orange-200 dark:bg-orange-500/10 dark:text-orange-300 dark:ring-orange-400/20',
  blue: 'bg-blue-50 text-blue-600 ring-blue-200 dark:bg-blue-500/10 dark:text-blue-300 dark:ring-blue-400/20',
  green:
    'bg-emerald-50 text-emerald-600 ring-emerald-200 dark:bg-emerald-500/10 dark:text-emerald-300 dark:ring-emerald-400/20',
  red: 'bg-red-50 text-red-600 ring-red-200 dark:bg-red-500/10 dark:text-red-300 dark:ring-red-400/20',
  slate:
    'bg-slate-50 text-slate-600 ring-slate-200 dark:bg-white/5 dark:text-slate-300 dark:ring-white/10',
};

const SummaryCard = ({ label, value, icon, tone }) => (
  <div className={`rounded-2xl p-4 ring-1 ${TONES[tone]}`}>
    <div className="flex items-center justify-between gap-3">
      <div>
        <p className="text-xs font-bold uppercase tracking-wider opacity-80">{label}</p>
        <p className="mt-1 text-2xl font-black text-slate-900 dark:text-white">{value}</p>
      </div>
      {createElement(icon, { size: 22, className: 'opacity-80' })}
    </div>
  </div>
);

const KitchenSummaryCards = ({ summary }) => (
  <div className="mt-6 grid grid-cols-2 gap-3 lg:grid-cols-5">
    <SummaryCard label="Chờ nấu" value={summary.pending} icon={Clock3} tone="orange" />
    <SummaryCard label="Đang nấu" value={summary.cooking} icon={Flame} tone="blue" />
    <SummaryCard label="Vừa hoàn thành" value={summary.finished} icon={CheckCircle2} tone="green" />
    <SummaryCard label="Món quá 20 phút" value={summary.overdue} icon={AlertTriangle} tone="red" />
    <SummaryCard
      label="Chờ trung bình"
      value={`${summary.averageWait} phút`}
      icon={Clock3}
      tone="slate"
    />
  </div>
);

export default KitchenSummaryCards;
