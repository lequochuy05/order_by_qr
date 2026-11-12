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
    <div className="mb-6 flex min-w-0 flex-col gap-4 rounded-2xl border border-[#E8DCD1] bg-white p-3 shadow-sm transition-colors dark:border-[#352C26] dark:bg-[#1F1A17] sm:p-4 md:flex-row md:items-center md:justify-between">
      <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
        <span className="mr-2 font-bold text-slate-700 dark:text-slate-200">{title}</span>
        <div className="grid min-w-0 grid-cols-[auto_minmax(0,1fr)] gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 dark:border-slate-700 dark:bg-slate-950 min-[480px]:flex min-[480px]:items-center">
          <Calendar size={18} className="text-slate-500 dark:text-slate-400" />
          <input
            type="date"
            aria-label="Từ ngày"
            className="min-w-0 bg-transparent text-sm font-medium text-slate-700 outline-none dark:text-slate-100"
            value={formatBusinessDate(dateRange.from)}
            onChange={(e) => handleDateChange('from', e.target.value)}
          />
          <span className="hidden text-slate-400 dark:text-slate-500 min-[480px]:inline">-</span>
          <input
            type="date"
            aria-label="Đến ngày"
            className="col-start-2 min-w-0 bg-transparent text-sm font-medium text-slate-700 outline-none dark:text-slate-100"
            value={formatBusinessDate(dateRange.to)}
            onChange={(e) => handleDateChange('to', e.target.value)}
          />
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setPreset(7)}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-bold text-slate-600 transition hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
          >
            7 ngày
          </button>
          <button
            type="button"
            onClick={() => setPreset(30)}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-bold text-slate-600 transition hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
          >
            30 ngày
          </button>
          {onApply && (
            <button
              type="button"
              onClick={onApply}
              className="flex items-center gap-1.5 rounded-xl bg-[#56372B] px-3.5 py-2 text-xs font-bold text-white shadow-sm transition-all hover:bg-[#432A21] dark:bg-[#C17C4A] dark:hover:bg-[#A86537]"
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
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 py-2 text-sm font-bold text-white transition hover:bg-emerald-700 sm:w-auto"
        >
          <Download size={16} /> Xuất CSV
        </button>
      )}
    </div>
  );
};

export default StatsToolbar;
