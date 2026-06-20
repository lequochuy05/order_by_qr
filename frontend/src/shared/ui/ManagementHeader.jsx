import { Plus, Search } from 'lucide-react';

const ManagementHeader = ({
  // Các Props để tùy chỉnh
  searchPlaceholder = 'Tìm kiếm...',
  searchTerm,
  setSearchTerm,
  onAddClick,
  addButtonText = 'Thêm mới',
  addButtonIcon: AddButtonIcon,

  // Phần mở rộng cho bộ lọc
  showFilter = false,
  filterAllLabel = 'Tất cả danh mục',
  filterValue,
  setFilterValue,
  filterOptions = [],
}) => (
  <div className="mb-6 grid min-w-0 grid-cols-1 gap-3 rounded-2xl border border-gray-100 bg-white p-3 shadow-sm sm:flex sm:flex-wrap sm:items-center sm:justify-between sm:gap-4 sm:p-4">
    <div className="flex min-w-0 flex-col gap-3 min-[420px]:flex-row min-[420px]:items-center sm:gap-4">
      {/* 1. Ô tìm kiếm */}
      {setSearchTerm && (
        <div className="relative min-w-0 flex-1 sm:w-64 sm:flex-none">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
          <input
            type="text"
            placeholder={searchPlaceholder}
            className="w-full pl-9 pr-4 py-1.5 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-500 outline-none text-sm"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      )}

      {/* 2. Bộ lọc Select */}
      {showFilter && (
        <select
          className="min-w-0 max-w-full rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm outline-none cursor-pointer focus:ring-2 focus:ring-orange-500 sm:py-1.5"
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
    <button
      onClick={onAddClick}
      className="flex min-w-0 items-center justify-center gap-2 rounded-lg bg-orange-500 px-4 py-2 text-sm font-bold text-white shadow-md transition-all hover:bg-orange-600 active:scale-95 sm:w-auto"
    >
      {AddButtonIcon ? (
        <AddButtonIcon className="shrink-0" size={18} />
      ) : (
        <Plus className="shrink-0" size={18} />
      )}
      <span className="truncate">{addButtonText}</span>
    </button>
  </div>
);

export default ManagementHeader;
