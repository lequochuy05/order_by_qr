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
    <div className="mb-6 flex min-w-0 flex-col gap-4 rounded-2xl border border-gray-100 bg-white p-3 shadow-sm sm:p-4 md:flex-row md:items-center md:justify-between">
      <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
        <span className="font-bold text-gray-700 mr-2">{title}</span>
        <div className="grid min-w-0 grid-cols-[auto_minmax(0,1fr)] gap-2 rounded-lg border bg-gray-50 px-3 py-2 min-[480px]:flex min-[480px]:items-center">
          <Calendar size={18} className="text-gray-500" />
          <input
            type="date"
            aria-label="Từ ngày"
            className="min-w-0 bg-transparent text-sm font-medium text-gray-700 outline-none"
            value={formatBusinessDate(dateRange.from)}
            onChange={(e) => handleDateChange('from', e.target.value)}
          />
          <span className="hidden text-gray-400 min-[480px]:inline">-</span>
          <input
            type="date"
            aria-label="Đến ngày"
            className="col-start-2 min-w-0 bg-transparent text-sm font-medium text-gray-700 outline-none"
            value={formatBusinessDate(dateRange.to)}
            onChange={(e) => handleDateChange('to', e.target.value)}
          />
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setPreset(7)}
            className="px-3 py-2 text-xs font-bold bg-white hover:bg-gray-200 border text-gray-600 rounded-lg"
          >
            7 ngày
          </button>
          <button
            type="button"
            onClick={() => setPreset(30)}
            className="px-3 py-2 text-xs font-bold bg-white hover:bg-gray-200 border text-gray-600 rounded-lg"
          >
            30 ngày
          </button>
          {onApply && (
            <button
              type="button"
              onClick={onApply}
              className="flex items-center gap-1.5 px-3 py-2 text-xs font-bold bg-indigo-500 border border-indigo-500 text-white hover:bg-indigo-600 rounded-lg shadow-sm shadow-indigo-100"
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
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-green-600 px-4 py-2 text-sm font-bold text-white hover:bg-green-700 sm:w-auto"
        >
          <Download size={16} /> Xuất CSV
        </button>
      )}
    </div>
  );
};

export default StatsToolbar;
