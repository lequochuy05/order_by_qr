import { Calendar, Download, Filter } from 'lucide-react';
import { formatBusinessDate } from '../lib/businessTime';

const StatsToolbar = ({ dateRange, setDateRange, onApply, onExport, title }) => {
  // Helper đổi ngày
  const handleDateChange = (type, value) => {
    const newDate = new Date(value);
    if (!isNaN(newDate.getTime())) {
      setDateRange((prev) => ({ ...prev, [type]: newDate }));
    }
  };

  const setPreset = (days) => {
    const to = new Date();
    to.setHours(0, 0, 0, 0);
    const from = new Date(to);
    from.setDate(to.getDate() - (days - 1));
    setDateRange({ from, to });
  };

  return (
    <div className="mb-6 flex min-w-0 flex-col gap-4 rounded-3xl border border-[#1E293B] bg-white/90 p-4 shadow-sm backdrop-blur-sm transition-colors dark:border-[#1E293B] dark:bg-[#0F172A]/90 sm:p-5 md:flex-row md:items-center md:justify-between">
      <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
        <span className="mr-2 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
          {title}
        </span>
        <div className="grid min-w-0 grid-cols-[auto_minmax(0,1fr)] gap-2 rounded-xl border border-[#1E293B]/20 bg-slate-50/80 px-3 py-2 dark:border-[#1E293B] dark:bg-slate-950/60 min-[480px]:flex min-[480px]:items-center">
          <Calendar size={16} className="text-slate-400 dark:text-slate-500" />
          <input
            type="date"
            aria-label="Từ ngày"
            className="min-w-0 bg-transparent text-xs font-semibold text-slate-800 outline-none dark:text-slate-100"
            value={formatBusinessDate(dateRange.from)}
            onChange={(e) => handleDateChange('from', e.target.value)}
          />
          <span className="hidden text-slate-400 dark:text-slate-500 min-[480px]:inline font-bold">-</span>
          <input
            type="date"
            aria-label="Đến ngày"
            className="col-start-2 min-w-0 bg-transparent text-xs font-semibold text-slate-800 outline-none dark:text-slate-100"
            value={formatBusinessDate(dateRange.to)}
            onChange={(e) => handleDateChange('to', e.target.value)}
          />
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setPreset(7)}
            className="rounded-xl border border-[#1E293B]/25 bg-white/90 px-3 py-2 text-xs font-bold text-slate-700 transition hover:bg-slate-50 dark:border-[#1E293B] dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700"
          >
            7 ngày
          </button>
          <button
            type="button"
            onClick={() => setPreset(30)}
            className="rounded-xl border border-[#1E293B]/25 bg-white/90 px-3 py-2 text-xs font-bold text-slate-700 transition hover:bg-slate-50 dark:border-[#1E293B] dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700"
          >
            30 ngày
          </button>
          {onApply && (
            <button
              type="button"
              onClick={onApply}
              className="flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3.5 py-2 text-xs font-bold text-white shadow-md shadow-indigo-600/20 transition-all hover:bg-indigo-700 active:scale-95 dark:bg-indigo-500 dark:hover:bg-indigo-600"
            >
              <Filter size={14} />
              Áp dụng
            </button>
          )}
        </div>
      </div>
      {onExport && (
        <button
          type="button"
          onClick={onExport}
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-emerald-600/20 transition-all hover:from-emerald-700 hover:to-teal-700 active:scale-95 sm:w-auto"
        >
          <Download size={14} /> Xuất CSV
        </button>
      )}
    </div>
  );
};

export default StatsToolbar;
