import { Loader2 } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';

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
  <div className="flex min-h-0 min-w-0 flex-1 flex-col border-b border-slate-200/80 bg-slate-50/50 transition-colors dark:border-slate-800/80 dark:bg-slate-950/60 md:border-b-0 md:border-r">
    <div className="border-b border-slate-200/80 bg-white p-5 transition-colors dark:border-slate-800/80 dark:bg-slate-900">
      <h3 className="mb-4 text-lg font-black text-slate-900 dark:text-slate-100">
        Thêm món - Bàn <span className="font-mono text-indigo-600 dark:text-indigo-400">{tableNumber}</span>
      </h3>
      <div className="mb-4 flex gap-2">
        {[
          ['ITEMS', 'Món lẻ'],
          ['COMBOS', 'Combo'],
        ].map(([value, label]) => (
          <button
            key={value}
            type="button"
            onClick={() => onTabChange(value)}
            className={`flex-1 rounded-xl py-2.5 text-xs font-black transition-all active:scale-95 sm:text-sm ${
              activeTab === value
                ? 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/25'
                : 'border border-slate-200/80 bg-slate-100/80 text-slate-600 hover:bg-slate-200/80 dark:border-slate-700/80 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {activeTab === 'ITEMS' && (
        <div className="flex gap-2 overflow-x-auto pb-1 custom-scrollbar">
          <button
            type="button"
            onClick={() => onCategoryChange('ALL')}
            className={`whitespace-nowrap rounded-xl border px-3.5 py-1.5 text-xs font-bold transition-all active:scale-95 ${
              selectedCategory === 'ALL'
                ? 'border-indigo-600 bg-indigo-50 text-indigo-700 shadow-sm dark:border-indigo-500/40 dark:bg-indigo-950/40 dark:text-indigo-300'
                : 'border-slate-200/80 bg-white text-slate-600 hover:bg-slate-50 dark:border-slate-700/80 dark:bg-slate-900 dark:text-slate-300'
            }`}
          >
            Tất cả
          </button>
          {categories.map((category) => (
            <button
              key={category.id}
              type="button"
              onClick={() => onCategoryChange(category.id)}
              className={`whitespace-nowrap rounded-xl border px-3.5 py-1.5 text-xs font-bold transition-all active:scale-95 ${
                selectedCategory === category.id
                  ? 'border-indigo-600 bg-indigo-50 text-indigo-700 shadow-sm dark:border-indigo-500/40 dark:bg-indigo-950/40 dark:text-indigo-300'
                  : 'border-slate-200/80 bg-white text-slate-600 hover:bg-slate-50 dark:border-slate-700/80 dark:bg-slate-900 dark:text-slate-300'
              }`}
            >
              {category.name}
            </button>
          ))}
        </div>
      )}
    </div>

    <div className="grid flex-1 grid-cols-1 content-start gap-3 overflow-y-auto p-4 custom-scrollbar min-[380px]:grid-cols-2 lg:grid-cols-3">
      {catalogLoading ? (
        <div className="col-span-full flex h-52 items-center justify-center text-slate-400 dark:text-slate-500">
          <Loader2 className="mr-2 h-5 w-5 animate-spin text-indigo-600 dark:text-indigo-400" />
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
              className={`flex min-h-[88px] flex-col justify-between rounded-2xl border bg-white p-3.5 text-left shadow-sm transition-all duration-200 dark:bg-slate-900 ${
                available
                  ? 'cursor-pointer border-slate-200/80 hover:-translate-y-0.5 hover:border-indigo-500 hover:shadow-md active:scale-95 dark:border-slate-800/80 dark:hover:border-indigo-500'
                  : 'cursor-not-allowed border-slate-200/40 opacity-55 dark:border-slate-800/40'
              }`}
            >
              <div className="text-sm font-bold leading-tight text-slate-800 dark:text-slate-100">
                {item.name}
              </div>
              <div className="mt-2.5 flex items-center justify-between gap-2">
                <span className="font-mono text-xs font-black text-indigo-600 dark:text-indigo-400">
                  {fmtVND(item.price)}
                </span>
                {!available && (
                  <span className="rounded-md bg-slate-100 px-2 py-0.5 text-[10px] font-black uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
                    Hết kho
                  </span>
                )}
              </div>
            </button>
          );
        })
      )}
      {!catalogLoading && displayList.length === 0 && (
        <div className="col-span-full flex h-52 items-center justify-center text-sm font-bold text-slate-400 dark:text-slate-500">
          Không có món phù hợp
        </div>
      )}
    </div>
  </div>
);

export default AddItemCatalogPane;
