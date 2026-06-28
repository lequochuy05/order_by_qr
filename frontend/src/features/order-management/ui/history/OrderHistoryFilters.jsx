import { Calendar, Filter, Hash, RefreshCcw, Search } from 'lucide-react';

import { ORDER_STATUS, getOrderStatusMeta } from '@entities/order/lib/orderStatus.js';
import { DATE_PRESETS } from '../../lib/orderHistoryDates.js';

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
            className={`rounded-xl border px-4 py-2 text-sm font-medium transition-all ${
              datePreset === preset.value
                ? 'border-orange-500 bg-orange-500 text-white shadow-md shadow-orange-200'
                : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50'
            }`}
          >
            {preset.label}
          </button>
        ))}
        <button
          type="button"
          onClick={() => onDatePresetChange('custom')}
          className={`flex items-center gap-1.5 rounded-xl border px-4 py-2 text-sm font-medium transition-all ${
            datePreset === 'custom'
              ? 'border-orange-500 bg-orange-500 text-white shadow-md shadow-orange-200'
              : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50'
          }`}
        >
          <Calendar size={14} /> Tùy chọn
        </button>
        <button
          type="button"
          onClick={onApply}
          className="flex items-center gap-1.5 rounded-xl border border-indigo-500 bg-indigo-500 px-4 py-2 text-sm font-medium text-white shadow-md shadow-indigo-200"
        >
          <Filter size={18} /> Áp dụng
        </button>
      </div>
      <button
        type="button"
        onClick={onRefresh}
        className="flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-5 py-2.5 font-medium text-gray-700 shadow-sm transition-all hover:bg-gray-50"
      >
        <RefreshCcw
          size={18}
          className={loading ? 'animate-spin text-orange-500' : 'text-gray-400'}
        />
        Làm mới
      </button>
    </div>

    {datePreset === 'custom' && (
      <div className="flex items-center gap-3 rounded-2xl border border-gray-100 bg-white p-4 shadow-sm">
        <Calendar size={18} className="text-gray-400" />
        <input
          type="date"
          value={customStartDate}
          onChange={(event) => onCustomStartDateChange(event.target.value)}
          className="rounded-xl border border-gray-200 bg-gray-50 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20"
        />
        <span className="text-gray-400">→</span>
        <input
          type="date"
          value={customEndDate}
          onChange={(event) => onCustomEndDateChange(event.target.value)}
          className="rounded-xl border border-gray-200 bg-gray-50 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20"
        />
      </div>
    )}

    <div className="grid grid-cols-1 gap-4 rounded-2xl border border-gray-100 bg-white p-5 shadow-sm md:grid-cols-12">
      <div className="group relative md:col-span-3">
        <Search
          className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 transition-colors group-focus-within:text-orange-500"
          size={18}
        />
        <input
          type="text"
          placeholder="Mã ĐH..."
          value={orderId}
          onChange={(event) => onOrderIdChange(event.target.value)}
          className="w-full rounded-xl border border-gray-200 bg-white py-3 pl-11 pr-4 text-sm text-gray-800 outline-none transition-all placeholder:text-gray-400 focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20"
        />
      </div>
      <div className="group relative md:col-span-3">
        <Hash
          className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 transition-colors group-focus-within:text-orange-500"
          size={18}
        />
        <input
          type="text"
          placeholder="Số bàn..."
          value={tableNumber}
          onChange={(event) => onTableNumberChange(event.target.value)}
          className="w-full rounded-xl border border-gray-200 bg-white py-3 pl-11 pr-4 text-sm text-gray-800 outline-none transition-all placeholder:text-gray-400 focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20"
        />
      </div>
      <div className="group relative md:col-span-4">
        <Filter
          className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 transition-colors group-focus-within:text-orange-500"
          size={18}
        />
        <select
          value={status}
          onChange={(event) => onStatusChange(event.target.value)}
          className="w-full cursor-pointer appearance-none rounded-xl border border-gray-200 bg-white py-3 pl-11 pr-4 text-sm text-gray-800 outline-none transition-all focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20"
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
