import { Calendar, Filter, Hash, RefreshCcw, Search } from 'lucide-react';

import { ORDER_STATUS, getOrderStatusMeta } from '@entities/order/lib/orderStatus.js';
import { DATE_PRESETS } from '@features/order-management/lib/orderHistoryDates.js';

const OrderHistoryFilters = ({
  orderId,
  tableNumber,
  status,
  datePreset,
  customStartDate,
  customEndDate,
  loading,
  onOrderIdChange,
  onTableNumberChange,
  onStatusChange,
  onDatePresetChange,
  onCustomStartDateChange,
  onCustomEndDateChange,
  onApply,
  onRefresh,
}) => (
  <>
    <div className="flex flex-col items-start justify-between gap-4 md:flex-row md:items-end">
      <div className="flex flex-wrap items-center gap-2">
        {DATE_PRESETS.map((preset) => (
          <button
            key={preset.value}
            type="button"
            onClick={() => onDatePresetChange(preset.value)}
            className={`rounded-xl px-4 py-2 text-xs font-bold transition-all active:scale-95 ${
              datePreset === preset.value
                ? 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/25'
                : 'border border-[#1E293B]/25 bg-white/90 text-slate-700 hover:border-indigo-400 hover:bg-indigo-50/50 hover:text-indigo-600 dark:border-[#1E293B] dark:bg-[#0F172A]/90 dark:text-slate-300 dark:hover:border-indigo-500'
            }`}
          >
            {preset.label}
          </button>
        ))}
        <button
          type="button"
          onClick={() => onDatePresetChange('custom')}
          className={`flex items-center gap-1.5 rounded-xl px-4 py-2 text-xs font-bold transition-all active:scale-95 ${
            datePreset === 'custom'
              ? 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/25'
              : 'border border-[#1E293B]/25 bg-white/90 text-slate-700 hover:border-indigo-400 hover:bg-indigo-50/50 hover:text-indigo-600 dark:border-[#1E293B] dark:bg-[#0F172A]/90 dark:text-slate-300 dark:hover:border-indigo-500'
          }`}
        >
          <Calendar size={14} /> Tùy chọn
        </button>
        <button
          type="button"
          onClick={onApply}
          className="flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-indigo-600/25 transition-all hover:from-indigo-500 hover:to-violet-500 active:scale-95"
        >
          <Filter size={16} /> Áp dụng
        </button>
      </div>
      <button
        type="button"
        onClick={onRefresh}
        className="flex items-center gap-2 rounded-xl border border-[#1E293B]/25 bg-white/90 px-4 py-2.5 text-xs font-bold text-slate-700 shadow-sm backdrop-blur-sm transition-all hover:bg-slate-50 active:scale-95 dark:border-[#1E293B] dark:bg-[#0F172A]/90 dark:text-slate-200 dark:hover:bg-slate-800/50"
      >
        <RefreshCcw
          size={16}
          className={loading ? 'animate-spin text-indigo-600' : 'text-slate-400 dark:text-slate-500'}
        />
        Làm mới
      </button>
    </div>

    {datePreset === 'custom' && (
      <div className="flex items-center gap-3 rounded-2xl border border-[#1E293B] bg-white/90 p-4 shadow-sm backdrop-blur-sm dark:border-[#1E293B] dark:bg-[#0F172A]/90">
        <Calendar size={18} className="text-slate-400 dark:text-slate-500" />
        <input
          type="date"
          value={customStartDate}
          onChange={(event) => onCustomStartDateChange(event.target.value)}
          className="rounded-xl border border-[#1E293B]/20 bg-slate-50 px-3 py-2 text-xs font-semibold outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-[#1E293B] dark:bg-slate-950 dark:text-slate-200"
        />
        <span className="text-slate-400 dark:text-slate-500">→</span>
        <input
          type="date"
          value={customEndDate}
          onChange={(event) => onCustomEndDateChange(event.target.value)}
          className="rounded-xl border border-[#1E293B]/20 bg-slate-50 px-3 py-2 text-xs font-semibold outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-[#1E293B] dark:bg-slate-950 dark:text-slate-200"
        />
      </div>
    )}

    <div className="grid grid-cols-1 gap-4 rounded-3xl border border-[#1E293B] bg-white/90 p-5 shadow-sm backdrop-blur-sm md:grid-cols-12 dark:border-[#1E293B] dark:bg-[#0F172A]/90">
      <div className="group relative md:col-span-3">
        <Search
          className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600 dark:text-slate-500 dark:group-focus-within:text-indigo-400"
          size={18}
        />
        <input
          type="text"
          placeholder="Mã ĐH..."
          value={orderId}
          onChange={(event) => onOrderIdChange(event.target.value)}
          className="w-full rounded-xl border border-[#1E293B]/20 bg-slate-50/70 py-2.5 pl-11 pr-4 text-xs font-semibold text-slate-800 outline-none transition-all placeholder:text-slate-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-[#1E293B] dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500"
        />
      </div>
      <div className="group relative md:col-span-3">
        <Hash
          className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600 dark:text-slate-500 dark:group-focus-within:text-indigo-400"
          size={18}
        />
        <input
          type="text"
          placeholder="Số bàn..."
          value={tableNumber}
          onChange={(event) => onTableNumberChange(event.target.value)}
          className="w-full rounded-xl border border-[#1E293B]/20 bg-slate-50/70 py-2.5 pl-11 pr-4 text-xs font-semibold text-slate-800 outline-none transition-all placeholder:text-slate-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-[#1E293B] dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500"
        />
      </div>
      <div className="group relative md:col-span-6">
        <Filter
          className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600 dark:text-slate-500 dark:group-focus-within:text-indigo-400"
          size={18}
        />
        <select
          value={status}
          onChange={(event) => onStatusChange(event.target.value)}
          className="w-full cursor-pointer appearance-none rounded-xl border border-[#1E293B]/20 bg-slate-50/70 py-2.5 pl-11 pr-4 text-xs font-semibold text-slate-800 outline-none transition-all focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-[#1E293B] dark:bg-slate-900 dark:text-slate-100"
        >
          <option value="">Tất cả trạng thái</option>
          {Object.keys(ORDER_STATUS).map((key) => (
            <option key={key} value={key}>
              {getOrderStatusMeta(key).label}
            </option>
          ))}
        </select>
      </div>
    </div>
  </>
);

export default OrderHistoryFilters;
