import { Plus, Search } from 'lucide-react';

const ManagementHeader = ({
  // Các Props để tùy chỉnh
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
  <div className="mb-6 grid min-w-0 grid-cols-1 gap-3 rounded-2xl border border-[#E8DCD1] bg-white p-3 shadow-sm transition-colors dark:border-[#352C26] dark:bg-[#1F1A17] sm:flex sm:flex-wrap sm:items-center sm:justify-between sm:gap-4 sm:p-4">
    <div className="flex min-w-0 flex-col gap-3 min-[420px]:flex-row min-[420px]:items-center sm:gap-4">
      {/* 1. Ô tìm kiếm */}
      {setSearchTerm && (
        <div className="relative min-w-0 flex-1 sm:w-64 sm:flex-none">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500" size={16} />
          <input
            type="text"
            placeholder={searchPlaceholder}
            className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2 pl-9 pr-4 text-sm font-medium text-slate-900 outline-none transition focus:border-[#C17C4A] focus:ring-2 focus:ring-[#C17C4A]/20 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      )}

      {/* 2. Bộ lọc Select */}
      {showFilter && (
        <select
          className="min-w-0 max-w-full cursor-pointer rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-medium text-slate-900 outline-none transition focus:border-[#C17C4A] focus:ring-2 focus:ring-[#C17C4A]/20 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
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
      className="flex min-w-0 items-center justify-center gap-2 rounded-xl bg-[#56372B] px-4 py-2.5 text-sm font-bold text-white shadow-sm transition-all hover:bg-[#432A21] active:scale-98 dark:bg-[#C17C4A] dark:hover:bg-[#A86537] sm:w-auto"
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
