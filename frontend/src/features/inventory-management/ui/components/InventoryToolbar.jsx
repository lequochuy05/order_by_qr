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
  <div className="rounded-3xl border border-gray-100 bg-white p-4 shadow-sm">
    <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
      <div className="grid min-w-0 grid-cols-2 gap-1 rounded-2xl bg-gray-50 p-1 sm:flex sm:items-center sm:gap-2">
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
          className="min-h-10 w-full min-w-0 rounded-xl border border-gray-200 bg-gray-50 px-3 text-sm font-bold text-gray-700 outline-none focus:ring-2 focus:ring-orange-500 disabled:cursor-wait disabled:opacity-60 md:w-64"
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
          className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-slate-800 px-4 text-sm font-black text-white transition-colors hover:bg-slate-700 disabled:opacity-50"
        >
          {menuItemsLoading ? (
            <Loader2 size={16} className="animate-spin" />
          ) : (
            <ClipboardList size={16} />
          )}{' '}
          Định mức
        </button>
        <button
          type="button"
          onClick={onRefresh}
          className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-gray-100 px-4 text-sm font-black text-gray-700 transition-colors hover:bg-gray-200"
        >
          <RefreshCw size={16} /> Đồng bộ
        </button>
      </div>
    </div>
  </div>
);

export default InventoryToolbar;
