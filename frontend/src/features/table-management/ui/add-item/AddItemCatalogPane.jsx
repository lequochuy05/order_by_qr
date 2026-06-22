import { Loader2 } from 'lucide-react';

const AddItemCatalogPane = ({
  tableNumber,
  activeTab,
  onTabChange,
  categories,
  selectedCategory,
  onCategoryChange,
  catalogLoading,
  displayList,
  onItemClick,
}) => (
  <div className="flex min-h-0 min-w-0 flex-1 flex-col border-b bg-gray-50 md:border-b-0 md:border-r">
    <div className="border-b bg-white p-4">
      <h3 className="mb-4 text-lg font-bold">Thêm món - Bàn {tableNumber}</h3>
      <div className="mb-4 flex gap-2">
        {[
          ['ITEMS', 'Món lẻ'],
          ['COMBOS', 'Combo'],
        ].map(([value, label]) => (
          <button
            key={value}
            type="button"
            onClick={() => onTabChange(value)}
            className={`flex-1 rounded-lg py-2 font-bold ${
              activeTab === value ? 'bg-orange-500 text-white' : 'bg-gray-200 text-gray-600'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {activeTab === 'ITEMS' && (
        <div className="flex gap-2 overflow-x-auto pb-2 custom-scrollbar">
          <button
            type="button"
            onClick={() => onCategoryChange('ALL')}
            className={`whitespace-nowrap rounded-full border px-3 py-1 text-sm ${
              selectedCategory === 'ALL'
                ? 'border-orange-500 bg-orange-100 text-orange-700'
                : 'bg-white'
            }`}
          >
            Tất cả
          </button>
          {categories.map((category) => (
            <button
              key={category.id}
              type="button"
              onClick={() => onCategoryChange(category.id)}
              className={`whitespace-nowrap rounded-full border px-3 py-1 text-sm ${
                selectedCategory === category.id
                  ? 'border-orange-500 bg-orange-100 text-orange-700'
                  : 'bg-white'
              }`}
            >
              {category.name}
            </button>
          ))}
        </div>
      )}
    </div>

    <div className="grid flex-1 grid-cols-1 content-start gap-3 overflow-y-auto p-3 min-[380px]:grid-cols-2 sm:p-4 lg:grid-cols-3">
      {catalogLoading ? (
        <div className="col-span-full flex h-52 items-center justify-center text-gray-400">
          <Loader2 className="mr-2 h-5 w-5 animate-spin text-orange-500" />
          <span className="text-sm font-bold">Đang tải món...</span>
        </div>
      ) : (
        displayList.map((item) => {
          const available = item.available !== false;
          return (
            <button
              key={item.id}
              type="button"
              disabled={!available}
              onClick={() => onItemClick(item, activeTab === 'ITEMS' ? 'ITEM' : 'COMBO')}
              className={`flex min-h-[80px] flex-col justify-between rounded-xl border bg-white p-3 text-left shadow-sm transition-all ${
                available
                  ? 'cursor-pointer hover:border-orange-500 hover:shadow-md active:scale-95'
                  : 'cursor-not-allowed opacity-55'
              }`}
            >
              <div className="text-sm font-bold leading-tight text-gray-800">{item.name}</div>
              <div className="mt-2 flex items-center justify-between gap-2">
                <span className="text-sm font-bold text-orange-600">
                  {item.price.toLocaleString()}đ
                </span>
                {!available && (
                  <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-black uppercase text-gray-500">
                    Hết kho
                  </span>
                )}
              </div>
            </button>
          );
        })
      )}
      {!catalogLoading && displayList.length === 0 && (
        <div className="col-span-full flex h-52 items-center justify-center text-sm font-bold text-gray-400">
          Không có món phù hợp
        </div>
      )}
    </div>
  </div>
);

export default AddItemCatalogPane;
