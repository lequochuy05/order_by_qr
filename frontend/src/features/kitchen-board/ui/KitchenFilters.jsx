import { createElement } from 'react';
import { AlertTriangle, Search, SlidersHorizontal, StickyNote } from 'lucide-react';

const FilterButton = ({ active, onClick, label, icon }) => (
  <button
    type="button"
    onClick={onClick}
    className={`inline-flex items-center gap-1.5 rounded-xl px-3.5 py-2 text-xs font-bold transition-all active:scale-95 ${
      active
        ? 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/25'
        : 'border border-slate-200/80 bg-slate-50/80 text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:border-slate-700/80 dark:bg-slate-800/80 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white'
    }`}
  >
    {icon && createElement(icon, { size: 14 })}
    {label}
  </button>
);

const KitchenFilters = ({
  searchTerm,
  categoryFilter,
  attentionFilter,
  categories,
  lastUpdatedAt,
  onSearchChange,
  onCategoryChange,
  onAttentionChange,
}) => (
  <section className="mt-5 rounded-3xl border border-[#1E293B] bg-white/90 p-4 shadow-sm backdrop-blur-sm dark:border-[#1E293B] dark:bg-[#0F172A]/90">
    <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
      <div className="flex flex-1 flex-col gap-3 sm:flex-row">
        <label className="relative block flex-1">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
          <input
            type="search"
            value={searchTerm}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Tìm theo bàn, món, mã đơn hoặc ghi chú..."
            className="w-full rounded-2xl border border-slate-200/80 bg-slate-50/80 py-2.5 pl-10 pr-4 text-sm font-medium outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700/80 dark:bg-slate-950/80 dark:focus:border-indigo-400 dark:focus:ring-indigo-500/20"
          />
        </label>
        <label className="relative block sm:w-56">
          <SlidersHorizontal
            className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"
            size={17}
          />
          <select
            value={categoryFilter}
            onChange={(event) => onCategoryChange(event.target.value)}
            className="w-full appearance-none rounded-2xl border border-slate-200/80 bg-slate-50/80 py-2.5 pl-10 pr-4 text-sm font-medium outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700/80 dark:bg-slate-950/80 dark:focus:border-indigo-400 dark:focus:ring-indigo-500/20"
          >
            <option value="ALL">Tất cả danh mục</option>
            {categories.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
        </label>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <FilterButton
          active={attentionFilter === 'ALL'}
          onClick={() => onAttentionChange('ALL')}
          label="Tất cả"
        />
        <FilterButton
          active={attentionFilter === 'OVERDUE'}
          onClick={() => onAttentionChange('OVERDUE')}
          label="Quá hạn"
          icon={AlertTriangle}
        />
        <FilterButton
          active={attentionFilter === 'NOTES'}
          onClick={() => onAttentionChange('NOTES')}
          label="Có ghi chú"
          icon={StickyNote}
        />
        <span className="ml-1 font-mono text-xs text-slate-400">
          {lastUpdatedAt
            ? `Cập nhật lúc ${lastUpdatedAt.toLocaleTimeString('vi-VN', {
                hour: '2-digit',
                minute: '2-digit',
              })}`
            : 'Đang đồng bộ...'}
        </span>
      </div>
    </div>
  </section>
);

export default KitchenFilters;
