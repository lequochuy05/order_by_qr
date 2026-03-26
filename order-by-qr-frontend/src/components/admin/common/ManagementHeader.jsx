import { Plus, Search } from 'lucide-react';

const ManagementHeader = ({
  // Các Props để tùy chỉnh
  title,
  searchPlaceholder = "Tìm kiếm...",
  searchTerm,
  setSearchTerm,
  onAddClick,
  addButtonText = "Thêm mới",
  addButtonIcon: AddButtonIcon,


  // Phần mở rộng cho bộ lọc 
  showFilter = false,
  filterValue,
  setFilterValue,
  filterOptions = []
}) => (
  <div className="flex flex-wrap items-center justify-between gap-4 bg-white p-4 rounded-2xl shadow-sm border border-gray-100 mb-6">
    <div className="flex items-center gap-4">
      {/* 1. Ô tìm kiếm */}
      {setSearchTerm && (
        <div className="relative w-64">
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
          className="px-3 py-1.5 bg-gray-50 border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-orange-500 cursor-pointer"
          value={filterValue}
          onChange={e => setFilterValue(e.target.value)}
        >
          <option value="ALL">Tất cả danh mục</option>
          {filterOptions.map(opt => (
            <option key={opt.id} value={opt.id}>{opt.name}</option>
          ))}
        </select>
      )}
    </div>

    {/* 3. Nút Thêm mới */}
    <button
      onClick={onAddClick}
      className="bg-orange-500 hover:bg-orange-600 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition-all shadow-md font-bold text-sm active:scale-95"
    >
      {AddButtonIcon ? <AddButtonIcon size={18} /> : <Plus size={18} />} {addButtonText}
    </button>
  </div>
);

export default ManagementHeader;