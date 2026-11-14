import { Boxes, ClipboardList, History, Loader2, RefreshCw } from 'lucide-react';

import TabButton from './TabButton.jsx';

const InventoryToolbar = ({
  activeTab,
  onTabChange,
  selectedMenuItemId,
  onMenuItemChange,
  onMenuItemFocus,
  menuItems,
  menuItemsLoading,
  onOpenRecipe,
  onRefresh,
}) => (
  <div className="rounded-3xl border border-[#1E293B] bg-white/90 p-4 shadow-sm backdrop-blur-sm transition-colors dark:border-[#1E293B] dark:bg-[#0F172A]/90">
    <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
      <div className="grid min-w-0 grid-cols-2 gap-1 rounded-2xl bg-slate-100 p-1 dark:bg-slate-800/70 sm:flex sm:items-center sm:gap-1.5">
        <TabButton
          active={activeTab === 'items'}
          icon={Boxes}
          label="Nguyên liệu"
          onClick={() => onTabChange('items')}
        />
        <TabButton
          active={activeTab === 'history'}
          icon={History}
          label="Lịch sử kho"
          onClick={() => onTabChange('history')}
        />
      </div>

      <div className="flex min-w-0 flex-col gap-3 md:flex-row md:items-center">
        <select
          value={selectedMenuItemId}
          onFocus={onMenuItemFocus}
          onChange={(e) => onMenuItemChange(e.target.value)}
          disabled={menuItemsLoading}
          className="min-h-10 w-full min-w-0 rounded-xl border border-slate-200/80 bg-white px-3.5 text-xs font-bold text-slate-800 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 disabled:cursor-wait disabled:opacity-60 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100 md:w-64"
        >
          {menuItemsLoading && <option value="">Đang tải món...</option>}
          {!menuItemsLoading && menuItems.length === 0 && (
            <option value="">Chọn món để định mức</option>
          )}
          {menuItems.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
        <button
          type="button"
          onClick={onOpenRecipe}
          disabled={menuItemsLoading}
          className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-4 text-xs font-bold text-white shadow-md shadow-indigo-600/25 transition-all hover:from-indigo-500 hover:to-violet-500 active:scale-95 disabled:opacity-50"
        >
          {menuItemsLoading ? (
            <Loader2 size={15} className="animate-spin" />
          ) : (
            <ClipboardList size={15} />
          )}{' '}
          Định mức
        </button>
        <button
          type="button"
          onClick={onRefresh}
          className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-slate-200/80 bg-slate-50 px-4 text-xs font-bold text-slate-700 transition-all hover:bg-slate-100 active:scale-95 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
        >
          <RefreshCw size={15} /> Đồng bộ
        </button>
      </div>
    </div>
  </div>
);

export default InventoryToolbar;
