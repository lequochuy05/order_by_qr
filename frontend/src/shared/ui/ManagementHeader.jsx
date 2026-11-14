import { Plus, Search } from 'lucide-react';

const ManagementHeader = ({
  // Các Props để tùy chỉnh
  title,
  searchPlaceholder = 'Tìm kiếm...',
  searchTerm,
  setSearchTerm,
  onAddClick,
  addButtonText = 'Thêm mới',
  addButtonIcon: AddButtonIcon,
  hideAddButton = false,

  // Phần mở rộng cho bộ lọc
  showFilter = false,
  filterAllLabel = 'Tất cả danh mục',
  filterValue,
  setFilterValue,
  filterOptions = [],
}) => (
  <div className="mb-6 grid min-w-0 grid-cols-1 gap-3 rounded-2xl border border-[#1E293B] bg-white/90 p-3 shadow-sm backdrop-blur-sm transition-colors dark:border-[#1E293B] dark:bg-[#0F172A]/90 sm:flex sm:flex-wrap sm:items-center sm:justify-between sm:gap-4 sm:p-4">
    <div className="flex min-w-0 flex-col gap-3 min-[420px]:flex-row min-[420px]:items-center sm:gap-4">
      {title && (
        <h2 className="text-lg font-black tracking-tight text-slate-900 dark:text-white sm:mr-1">
          {title}
        </h2>
      )}
      {/* 1. Ô tìm kiếm */}
      {setSearchTerm && (
        <div className="relative min-w-0 flex-1 sm:w-64 sm:flex-none">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500" size={16} />
          <input
            type="text"
            placeholder={searchPlaceholder}
            className="w-full rounded-xl border border-[#1E293B]/20 bg-slate-50/70 py-2 pl-9 pr-4 text-sm font-medium text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-[#1E293B] dark:bg-slate-950/70 dark:text-slate-100"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      )}

      {/* 2. Bộ lọc Select */}
      {showFilter && (
        <select
          className="min-w-0 max-w-full cursor-pointer rounded-xl border border-[#1E293B]/20 bg-slate-50/70 px-3 py-2 text-sm font-medium text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-[#1E293B] dark:bg-slate-950/70 dark:text-slate-100"
          value={filterValue}
          onChange={(e) => setFilterValue(e.target.value)}
        >
          <option value="ALL">{filterAllLabel}</option>
          {filterOptions.map((opt) => (
            <option key={opt.id} value={opt.id}>
              {opt.name}
            </option>
          ))}
        </select>
      )}
    </div>

    {/* 3. Nút Thêm mới */}
    {!hideAddButton && (
    <button
      onClick={onAddClick}
      className="flex min-w-0 items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-4 py-2.5 text-sm font-bold text-white shadow-md shadow-indigo-600/25 transition-all hover:from-indigo-500 hover:to-violet-500 active:scale-98 sm:w-auto"
    >
      {AddButtonIcon ? (
        <AddButtonIcon className="shrink-0" size={18} />
      ) : (
        <Plus className="shrink-0" size={18} />
      )}
      <span className="truncate">{addButtonText}</span>
    </button>
    )}
  </div>
);

export default ManagementHeader;
