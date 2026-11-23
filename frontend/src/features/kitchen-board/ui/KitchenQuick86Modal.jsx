import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Ban,
  BookOpen,
  CheckCircle2,
  Loader2,
  RefreshCw,
  Search,
  UtensilsCrossed,
} from 'lucide-react';

import { comboService } from '@entities/combo';
import { menuItemService } from '@entities/menu-item';
import { fmtVND } from '@shared/lib/formatters.js';
import { useTranslation } from '@shared/lib/i18n';
import { ModalHeader, SharedModal } from '@shared/ui';
import { getAvailabilityTargetKey } from '../model/useKitchenOrders.js';

const KitchenQuick86Modal = ({
  isOpen,
  onClose,
  onToggleAvailability,
  onOpenRecipe,
  togglingAvailabilityKeys = new Set(),
  lastAvailabilityUpdate = null,
}) => {
  const { isEn } = useTranslation('kitchen');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [catalogItems, setCatalogItems] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterMode, setFilterMode] = useState('ALL');

  const loadCatalog = useCallback(
    async ({ silent = false } = {}) => {
      if (!silent) {
        setLoading(true);
      }
      setError(null);
      try {
        const [menuItems, combos] = await Promise.all([
          menuItemService.getAll(null, { force: true }),
          comboService.getAll({ force: true }),
        ]);

        const normalizedMenu = (Array.isArray(menuItems) ? menuItems : [])
          .filter((item) => item.active !== false)
          .map((item) => ({
            key: `menu-${item.id}`,
            menuItemId: item.id,
            comboId: null,
            isCombo: false,
            itemName: item.name,
            category: item.category?.name || (isEn ? 'Other' : 'Khác'),
            price: item.price,
            isAvailable: item.available !== false,
            version: item.version,
          }));

        const normalizedCombos = (Array.isArray(combos) ? combos : [])
          .filter((combo) => combo.active !== false)
          .map((combo) => ({
            key: `combo-${combo.id}`,
            menuItemId: null,
            comboId: combo.id,
            isCombo: true,
            itemName: combo.name,
            category: 'Combo',
            price: combo.price,
            isAvailable: combo.available !== false,
            version: combo.version,
          }));

        setCatalogItems([...normalizedMenu, ...normalizedCombos]);
      } catch (err) {
        console.error('Failed to load catalog for stock control modal:', err);
        if (!silent) {
          setError(
            isEn
              ? 'Could not load menu catalog. Please try again.'
              : 'Không thể tải danh sách thực đơn. Vui lòng thử lại.',
          );
        }
      } finally {
        if (!silent) {
          setLoading(false);
        }
      }
    },
    [isEn],
  );

  useEffect(() => {
    if (!isOpen) return;
    loadCatalog();
  }, [isOpen, loadCatalog]);

  useEffect(() => {
    if (!isOpen || !lastAvailabilityUpdate?.key) return;
    setCatalogItems((prev) =>
      prev.map((item) =>
        item.key === lastAvailabilityUpdate.key
          ? { ...item, isAvailable: lastAvailabilityUpdate.isAvailable }
          : item,
      ),
    );
    loadCatalog({ silent: true });
  }, [isOpen, lastAvailabilityUpdate, loadCatalog]);

  const outOfStockCount = useMemo(
    () => catalogItems.filter((item) => !item.isAvailable).length,
    [catalogItems],
  );

  const filteredItems = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();

    return catalogItems
      .filter((item) => {
        if (filterMode === 'OUT_OF_STOCK' && item.isAvailable) return false;
        if (filterMode === 'AVAILABLE' && !item.isAvailable) return false;
        if (filterMode === 'COMBO' && !item.isCombo) return false;

        if (!query) return true;
        return (
          item.itemName?.toLowerCase().includes(query) ||
          item.category?.toLowerCase().includes(query)
        );
      })
      .sort((a, b) => {
        if (a.isAvailable !== b.isAvailable) return a.isAvailable ? 1 : -1;
        return (a.itemName || '').localeCompare(b.itemName || '', 'vi');
      });
  }, [catalogItems, filterMode, searchTerm]);

  const handleToggleItem = async (entry) => {
    if (!onToggleAvailability) return;
    const nextAvailable = await onToggleAvailability(entry);
    if (nextAvailable !== null) {
      setCatalogItems((prev) =>
        prev.map((item) =>
          item.key === entry.key
            ? {
                ...item,
                isAvailable: nextAvailable,
                version: item.version != null ? item.version + 1 : item.version,
              }
            : item,
        ),
      );
      await loadCatalog({ silent: true });
    }
  };

  const filterTabs = [
    { id: 'ALL', label: isEn ? `All (${catalogItems.length})` : `Tất cả (${catalogItems.length})` },
    {
      id: 'OUT_OF_STOCK',
      label: isEn ? `Out of stock (${outOfStockCount})` : `Đang tạm hết (${outOfStockCount})`,
    },
    {
      id: 'AVAILABLE',
      label: isEn ? 'Available' : 'Đang bán',
    },
    {
      id: 'COMBO',
      label: 'Combo',
    },
  ];

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      closeOnBackdrop
      className="max-w-3xl !p-0"
      ariaLabel={isEn ? 'Quick Out-of-Stock Control' : 'Báo hết món nhanh tại bếp'}
    >
      <ModalHeader
        title={isEn ? 'Quick Out-of-Stock Control' : 'Báo hết món nhanh tại bếp'}
        subtitle={isEn ? 'Kitchen Stock Control' : 'Điều phối trạng thái món thời gian thực'}
        icon={Ban}
        onClose={onClose}
        badge={
          outOfStockCount > 0 ? (
            <span className="shrink-0 rounded-full border border-rose-200 bg-rose-50 px-2.5 py-0.5 text-[11px] font-black text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/15 dark:text-rose-300">
              {outOfStockCount} {isEn ? 'out of stock' : 'đang tạm hết'}
            </span>
          ) : null
        }
      />

      <div className="space-y-3 border-b border-slate-200/80 bg-slate-50/70 px-5 py-3.5 dark:border-slate-800 dark:bg-slate-900/60 sm:px-7">
        <div className="flex flex-col gap-2.5 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search
              size={16}
              className="pointer-events-none absolute top-1/2 left-3.5 -translate-y-1/2 text-slate-400"
            />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder={
                isEn ? 'Search dish or category...' : 'Tìm theo tên món hoặc danh mục...'
              }
              className="w-full rounded-xl border border-slate-200 bg-white py-2 pr-3 pl-9 text-sm font-medium text-slate-800 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
            />
          </div>

          <button
            type="button"
            onClick={() => loadCatalog()}
            disabled={loading}
            className="inline-flex shrink-0 items-center justify-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-bold text-slate-700 transition hover:bg-slate-100 disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            {isEn ? 'Reload' : 'Tải lại'}
          </button>
        </div>

        <div className="flex flex-wrap gap-1.5">
          {filterTabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setFilterMode(tab.id)}
              className={`rounded-xl px-3 py-1.5 text-xs font-extrabold transition ${
                filterMode === tab.id
                  ? tab.id === 'OUT_OF_STOCK'
                    ? 'bg-rose-600 text-white shadow-xs'
                    : 'bg-indigo-600 text-white shadow-xs'
                  : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      <div className="max-h-[54vh] flex-1 overflow-y-auto px-5 py-4 sm:px-7">
        {loading ? (
          <div className="flex min-h-48 flex-col items-center justify-center gap-3 text-slate-400">
            <Loader2 size={32} className="animate-spin text-indigo-600 dark:text-indigo-400" />
            <p className="text-sm font-semibold">
              {isEn ? 'Loading menu catalog...' : 'Đang tải danh sách món ăn & combo...'}
            </p>
          </div>
        ) : error ? (
          <div className="rounded-2xl border border-rose-200 bg-rose-50 p-5 text-center dark:border-rose-500/30 dark:bg-rose-950/20">
            <p className="text-sm font-bold text-rose-700 dark:text-rose-300">{error}</p>
            <button
              type="button"
              onClick={() => loadCatalog()}
              className="mt-3 rounded-xl bg-rose-600 px-4 py-1.5 text-xs font-bold text-white transition hover:bg-rose-500"
            >
              {isEn ? 'Retry' : 'Thử lại'}
            </button>
          </div>
        ) : filteredItems.length === 0 ? (
          <div className="flex min-h-48 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50/70 p-6 text-center dark:border-slate-800 dark:bg-slate-900/40">
            <UtensilsCrossed size={28} className="mb-2 text-slate-400" />
            <p className="text-sm font-bold text-slate-600 dark:text-slate-300">
              {isEn ? 'No matching dishes found.' : 'Không tìm thấy món phù hợp.'}
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {filteredItems.map((entry) => {
              const targetKey = getAvailabilityTargetKey(entry);
              const isToggling = Boolean(targetKey && togglingAvailabilityKeys.has(targetKey));

              return (
                <div
                  key={entry.key}
                  className={`flex flex-wrap items-center justify-between gap-3 rounded-2xl border px-4 py-3 transition ${
                    entry.isAvailable
                      ? 'border-slate-200/80 bg-white hover:border-slate-300 dark:border-slate-800 dark:bg-slate-900/70'
                      : 'border-rose-200 bg-rose-50/70 dark:border-rose-500/30 dark:bg-rose-950/20'
                  }`}
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-md bg-slate-100 px-2 py-0.5 text-[10px] font-black uppercase tracking-wider text-slate-500 dark:bg-slate-800 dark:text-slate-400">
                        {entry.category}
                      </span>
                      {!entry.isAvailable && (
                        <span className="inline-flex items-center gap-1 rounded-md bg-rose-100 px-2 py-0.5 text-[10px] font-black uppercase text-rose-700 dark:bg-rose-500/20 dark:text-rose-300">
                          <Ban size={11} />
                          {isEn ? 'Out of stock' : 'Tạm hết món'}
                        </span>
                      )}
                    </div>
                    <p className="mt-1 truncate text-sm font-black text-slate-800 dark:text-slate-100">
                      {entry.itemName}
                    </p>
                    <p className="text-xs font-mono font-semibold text-slate-400">
                      {fmtVND(entry.price)}
                    </p>
                  </div>

                  <div className="flex items-center gap-2">
                    {onOpenRecipe && (
                      <button
                        type="button"
                        onClick={() =>
                          onOpenRecipe({
                            menuItemId: entry.menuItemId,
                            comboId: entry.comboId,
                            isCombo: entry.isCombo,
                            itemName: entry.itemName,
                            category: entry.category,
                            quantity: 1,
                            isAvailable: entry.isAvailable,
                          })
                        }
                        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-bold text-slate-700 transition hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
                        title={isEn ? 'View recipe & stock' : 'Xem công thức & định lượng'}
                      >
                        <BookOpen size={14} />
                        {isEn ? 'Recipe' : 'Công thức'}
                      </button>
                    )}

                    <button
                      type="button"
                      onClick={() => handleToggleItem(entry)}
                      disabled={isToggling}
                      className={`inline-flex min-w-32 items-center justify-center gap-1.5 rounded-xl px-3.5 py-2 text-xs font-black transition active:scale-95 disabled:cursor-not-allowed disabled:opacity-60 ${
                        entry.isAvailable
                          ? 'bg-rose-600 text-white shadow-xs hover:bg-rose-500'
                          : 'bg-emerald-600 text-white shadow-xs hover:bg-emerald-500'
                      }`}
                    >
                      {isToggling ? (
                        <Loader2 size={14} className="animate-spin" />
                      ) : entry.isAvailable ? (
                        <Ban size={14} />
                      ) : (
                        <CheckCircle2 size={14} />
                      )}
                      {entry.isAvailable
                        ? isEn
                          ? 'Mark out of stock'
                          : 'Báo hết món'
                        : isEn
                          ? 'Re-enable'
                          : 'Mở bán lại'}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <div className="flex items-center justify-between gap-3 border-t border-slate-200/80 bg-slate-50/70 px-6 py-3.5 dark:border-slate-800 dark:bg-slate-900/80">
        <p className="text-xs text-slate-400">
          {isEn
            ? "Changes sync immediately to QR menus and staff POS."
            : 'Thay đổi có hiệu lực tức thì trên thực đơn QR của khách và màn hình phục vụ.'}
        </p>
        <button
          type="button"
          onClick={onClose}
          className="rounded-xl border border-slate-200 bg-white px-5 py-2 text-sm font-bold text-slate-700 shadow-xs transition hover:bg-slate-100 active:scale-95 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
        >
          {isEn ? 'Close' : 'Đóng'}
        </button>
      </div>
    </SharedModal>
  );
};

export default KitchenQuick86Modal;
