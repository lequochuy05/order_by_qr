import { createElement } from 'react';
import { AlertTriangle, Search, SlidersHorizontal, StickyNote } from 'lucide-react';

const FilterButton = ({ active, onClick, label, icon }) => (
  <button
    type="button"
    onClick={onClick}
    className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-bold transition ${
      active
        ? 'bg-slate-900 text-white shadow-sm dark:bg-orange-500'
        : 'bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700'
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
  <section className="mt-5 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
    <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
      <div className="flex flex-1 flex-col gap-3 sm:flex-row">
        <label className="relative block flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
          <input
            type="search"
            value={searchTerm}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Tìm theo bàn, món, mã đơn hoặc ghi chú..."
            className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-10 pr-4 text-sm outline-none transition focus:border-orange-400 focus:ring-2 focus:ring-orange-100 dark:border-slate-700 dark:bg-slate-950 dark:focus:ring-orange-500/10"
          />
        </label>
        <label className="relative block sm:w-56">
          <SlidersHorizontal
            className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
            size={17}
          />
          <select
            value={categoryFilter}
            onChange={(event) => onCategoryChange(event.target.value)}
            className="w-full appearance-none rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-10 pr-4 text-sm font-medium outline-none transition focus:border-orange-400 dark:border-slate-700 dark:bg-slate-950"
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
        <span className="ml-1 text-xs text-slate-400">
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
