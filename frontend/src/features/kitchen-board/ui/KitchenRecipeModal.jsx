import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  Ban,
  BookOpen,
  CheckCircle2,
  ChefHat,
  Loader2,
  Minus,
  Package,
  Plus,
  RefreshCw,
} from 'lucide-react';

import { comboService } from '@entities/combo';
import { inventoryService } from '@entities/inventory';
import { useTranslation } from '@shared/lib/i18n';
import { ModalHeader, SharedModal } from '@shared/ui';
import { scaleRecipeItems } from '../lib/kitchenItems.js';

const formatQty = (value) => {
  const num = Number(value || 0);
  if (!Number.isFinite(num)) return '0';
  return Number(num.toFixed(3)).toString();
};

const RecipeTable = ({ rows, multiplier, isEn }) => {
  if (!rows || rows.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50/70 px-4 py-8 text-center dark:border-slate-800 dark:bg-slate-900/40">
        <Package size={26} className="mb-2 text-slate-400" />
        <p className="text-sm font-bold text-slate-600 dark:text-slate-300">
          {isEn
            ? 'No recipe ingredients configured for this dish.'
            : 'Món này chưa được thiết lập định lượng nguyên liệu (BOM).'}
        </p>
        <p className="mt-1 text-xs text-slate-400">
          {isEn
            ? 'Managers can configure recipe_items in Inventory Management.'
            : 'Quản lý có thể cấu hình công thức trong mục Quản lý Kho.'}
        </p>
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200/80 bg-white dark:border-slate-800 dark:bg-slate-900/70">
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-slate-100 bg-slate-50/90 text-[11px] font-black uppercase tracking-wider text-slate-500 dark:border-slate-800 dark:bg-slate-800/60 dark:text-slate-400">
              <th className="px-4 py-3">{isEn ? 'Ingredient' : 'Nguyên liệu'}</th>
              <th className="px-4 py-3 text-right">{isEn ? 'Per 1 portion' : 'Định mức (x1)'}</th>
              <th className="px-4 py-3 text-right">
                {isEn ? `Need (x${multiplier})` : `Tổng cần (x${multiplier})`}
              </th>
              <th className="px-4 py-3 text-right">{isEn ? 'In Stock' : 'Tồn khả dụng'}</th>
              <th className="px-4 py-3 text-right">{isEn ? 'Status' : 'Trạng thái'}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
            {rows.map((row) => {
              let badgeClass =
                'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200/70 dark:bg-emerald-500/10 dark:text-emerald-300 dark:ring-emerald-500/20';
              let badgeLabel = isEn ? 'Sufficient' : 'Đủ dùng';

              if (!row.hasStockInfo) {
                badgeClass = 'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400';
                badgeLabel = isEn ? 'N/A' : 'Chưa rõ';
              } else if (row.isOutOfStock) {
                badgeClass =
                  'bg-rose-50 text-rose-700 ring-1 ring-rose-200 dark:bg-rose-500/10 dark:text-rose-300 dark:ring-rose-500/30';
                badgeLabel = isEn ? 'Out of stock' : 'Hết kho';
              } else if (row.isInsufficientStock) {
                badgeClass =
                  'bg-rose-50 text-rose-700 ring-1 ring-rose-200 dark:bg-rose-500/10 dark:text-rose-300 dark:ring-rose-500/30';
                badgeLabel = isEn ? 'Shortage' : 'Thiếu hàng';
              } else if (row.isLowStock) {
                badgeClass =
                  'bg-amber-50 text-amber-700 ring-1 ring-amber-200 dark:bg-amber-500/10 dark:text-amber-300 dark:ring-amber-500/30';
                badgeLabel = isEn ? 'Low stock' : 'Sắp hết';
              }

              return (
                <tr
                  key={`${row.id}-${row.inventoryItemId}`}
                  className={`transition-colors hover:bg-slate-50/70 dark:hover:bg-slate-800/40 ${
                    row.isInsufficientStock || row.isOutOfStock
                      ? 'bg-rose-50/30 dark:bg-rose-950/10'
                      : ''
                  }`}
                >
                  <td className="px-4 py-3 font-bold text-slate-800 dark:text-slate-100">
                    {row.inventoryItemName || `#${row.inventoryItemId}`}
                  </td>
                  <td className="px-4 py-3 text-right font-mono text-xs font-semibold text-slate-500 dark:text-slate-400">
                    {formatQty(row.quantityRequired)} {row.unit}
                  </td>
                  <td className="px-4 py-3 text-right font-mono text-sm font-black text-indigo-700 dark:text-indigo-400">
                    {formatQty(row.scaledQuantity)} {row.unit}
                  </td>
                  <td className="px-4 py-3 text-right font-mono text-xs font-bold text-slate-600 dark:text-slate-300">
                    {row.hasStockInfo ? `${formatQty(row.availableQuantity)} ${row.unit}` : '—'}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <span
                      className={`inline-flex items-center rounded-lg px-2 py-0.5 text-[11px] font-extrabold ${badgeClass}`}
                    >
                      {badgeLabel}
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

const KitchenRecipeModal = ({
  isOpen,
  onClose,
  target,
  onToggleAvailability,
  isTogglingAvailability = false,
}) => {
  const { isEn } = useTranslation('kitchen');
  const initialQty = Math.max(1, Number(target?.quantity) || 1);
  const [portionMultiplier, setPortionMultiplier] = useState(initialQty);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [singleRecipes, setSingleRecipes] = useState([]);
  const [comboSections, setComboSections] = useState([]);

  useEffect(() => {
    if (isOpen && target) {
      setPortionMultiplier(Math.max(1, Number(target.quantity) || 1));
    }
  }, [isOpen, target]);

  const loadRecipes = useCallback(async () => {
    if (!target || (!target.menuItemId && !target.comboId)) {
      setSingleRecipes([]);
      setComboSections([]);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      if (target.isCombo && target.comboId) {
        const combo = await comboService.getById(target.comboId, { force: true });
        const comboItems = Array.isArray(combo?.items) ? combo.items : [];
        const sections = await Promise.all(
          comboItems.map(async (ci) => {
            const items = ci.menuItemId
              ? await inventoryService.getRecipe(ci.menuItemId, { force: true })
              : [];
            return {
              ...ci,
              recipeItems: Array.isArray(items) ? items : [],
            };
          }),
        );
        setComboSections(sections);
        setSingleRecipes([]);
      } else if (target.menuItemId) {
        const items = await inventoryService.getRecipe(target.menuItemId, { force: true });
        setSingleRecipes(Array.isArray(items) ? items : []);
        setComboSections([]);
      }
    } catch (err) {
      console.error('Failed to load kitchen recipe:', err);
      setError(
        isEn
          ? 'Could not load recipe ingredients. Please try again.'
          : 'Không thể tải công thức định lượng. Vui lòng thử lại.',
      );
    } finally {
      setLoading(false);
    }
  }, [isEn, target]);

  useEffect(() => {
    if (!isOpen || !target) return;
    loadRecipes();
  }, [isOpen, target, loadRecipes]);

  const scaledSingleRecipes = useMemo(
    () => scaleRecipeItems(singleRecipes, portionMultiplier),
    [singleRecipes, portionMultiplier],
  );

  const scaledComboSections = useMemo(
    () =>
      comboSections.map((section) => {
        const effectiveMultiplier = Math.max(1, Number(section.quantity) || 1) * portionMultiplier;
        return {
          ...section,
          effectiveMultiplier,
          scaledRows: scaleRecipeItems(section.recipeItems, effectiveMultiplier),
        };
      }),
    [comboSections, portionMultiplier],
  );

  const shortageCount = useMemo(() => {
    if (target?.isCombo && target?.comboId) {
      return scaledComboSections.reduce(
        (count, section) =>
          count + section.scaledRows.filter((r) => r.isInsufficientStock || r.isOutOfStock).length,
        0,
      );
    }
    return scaledSingleRecipes.filter((r) => r.isInsufficientStock || r.isOutOfStock).length;
  }, [scaledComboSections, scaledSingleRecipes, target]);

  const isAvailable = target?.isAvailable !== false;
  const canToggleAvailability = Boolean(onToggleAvailability && (target?.menuItemId || target?.comboId));

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      closeOnBackdrop
      className="max-w-3xl !p-0"
      ariaLabel={isEn ? 'Kitchen Recipe Viewer' : 'Xem nhanh công thức bếp'}
    >
      <ModalHeader
        title={target?.itemName || (isEn ? 'Dish Recipe' : 'Công thức món ăn')}
        subtitle={
          target?.isCombo
            ? isEn
              ? 'Combo Recipe Breakdown'
              : 'Công thức định lượng Combo'
            : isEn
              ? 'Kitchen BOM / Recipe Viewer'
              : 'Định lượng & Công thức chế biến'
        }
        icon={BookOpen}
        onClose={onClose}
        badge={
          target?.category ? (
            <span className="shrink-0 rounded-full border border-indigo-200 bg-indigo-50 px-2.5 py-0.5 text-[11px] font-black text-indigo-700 dark:border-indigo-500/30 dark:bg-indigo-500/15 dark:text-indigo-300">
              {target.category}
            </span>
          ) : null
        }
      />

      <div className="flex-1 space-y-4 overflow-y-auto px-5 py-4 sm:px-7 sm:py-5">
        {/* Controls Bar: Multiplier + Availability 86'd Toggle */}
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200/80 bg-slate-50/80 p-3.5 dark:border-slate-800 dark:bg-slate-800/40">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs font-extrabold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              {isEn ? 'Scale portions:' : 'Tính cho số phần:'}
            </span>

            <div className="inline-flex items-center rounded-xl border border-slate-200 bg-white p-1 dark:border-slate-700 dark:bg-slate-900">
              <button
                type="button"
                onClick={() => setPortionMultiplier(1)}
                className={`rounded-lg px-2.5 py-1 text-xs font-black transition ${
                  portionMultiplier === 1
                    ? 'bg-indigo-600 text-white shadow-xs'
                    : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'
                }`}
              >
                {isEn ? 'Standard (x1)' : '1 phần (x1)'}
              </button>
              {initialQty > 1 && (
                <button
                  type="button"
                  onClick={() => setPortionMultiplier(initialQty)}
                  className={`rounded-lg px-2.5 py-1 text-xs font-black transition ${
                    portionMultiplier === initialQty
                      ? 'bg-indigo-600 text-white shadow-xs'
                      : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'
                  }`}
                >
                  {isEn ? `Ticket (x${initialQty})` : `Theo thẻ (x${initialQty})`}
                </button>
              )}
            </div>

            <div className="inline-flex items-center gap-1 rounded-xl border border-slate-200 bg-white px-1.5 py-1 dark:border-slate-700 dark:bg-slate-900">
              <button
                type="button"
                onClick={() => setPortionMultiplier((prev) => Math.max(1, prev - 1))}
                disabled={portionMultiplier <= 1}
                className="flex h-6 w-6 items-center justify-center rounded-lg text-slate-600 transition hover:bg-slate-100 disabled:opacity-40 dark:text-slate-300 dark:hover:bg-slate-800"
                aria-label={isEn ? 'Decrease quantity' : 'Giảm số phần'}
              >
                <Minus size={14} />
              </button>
              <span className="min-w-8 text-center font-mono text-xs font-black text-indigo-700 dark:text-indigo-400">
                x{portionMultiplier}
              </span>
              <button
                type="button"
                onClick={() => setPortionMultiplier((prev) => prev + 1)}
                className="flex h-6 w-6 items-center justify-center rounded-lg text-slate-600 transition hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
                aria-label={isEn ? 'Increase quantity' : 'Tăng số phần'}
              >
                <Plus size={14} />
              </button>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={loadRecipes}
              disabled={loading}
              className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-bold text-slate-600 transition hover:bg-slate-100 disabled:opacity-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
              title={isEn ? 'Refresh stock quantities' : 'Cập nhật tồn kho mới nhất'}
            >
              <RefreshCw size={13} className={loading ? 'animate-spin' : ''} />
              {isEn ? 'Refresh stock' : 'Làm mới kho'}
            </button>

            {canToggleAvailability && (
              <button
                type="button"
                onClick={() => onToggleAvailability(target)}
                disabled={isTogglingAvailability}
                className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-black transition active:scale-95 disabled:opacity-50 ${
                  isAvailable
                    ? 'border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 dark:border-rose-500/30 dark:bg-rose-500/15 dark:text-rose-300 dark:hover:bg-rose-500/25'
                    : 'border border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 dark:border-emerald-500/30 dark:bg-emerald-500/15 dark:text-emerald-300 dark:hover:bg-emerald-500/25'
                }`}
              >
                {isTogglingAvailability ? (
                  <Loader2 size={13} className="animate-spin" />
                ) : isAvailable ? (
                  <Ban size={13} />
                ) : (
                  <CheckCircle2 size={13} />
                )}
                {isAvailable
                  ? isEn
                    ? "86'd (Mark Out of Stock)"
                    : "Báo hết (86'd)"
                  : isEn
                    ? 'Re-enable Dish'
                    : 'Mở bán lại'}
              </button>
            )}
          </div>
        </div>

        {/* Shortage / 86'd Status Banners */}
        {!isAvailable && (
          <div className="flex items-center gap-2.5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs font-bold text-rose-800 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-300">
            <Ban size={16} className="shrink-0" />
            <span>
              {isEn
                ? "This item is currently marked 86'd (temporarily out of stock) on the customer menu."
                : "Món này hiện đang tạm khóa (86'd) trên thực đơn khách hàng."}
            </span>
          </div>
        )}

        {!loading && shortageCount > 0 && (
          <div className="flex items-center gap-2.5 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-xs font-bold text-amber-800 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-300">
            <AlertTriangle size={16} className="shrink-0" />
            <span>
              {isEn
                ? `Warning: ${shortageCount} ingredient(s) are out of stock or insufficient for x${portionMultiplier} portion(s).`
                : `Cảnh báo: Có ${shortageCount} nguyên liệu đang hết hoặc không đủ tồn kho cho x${portionMultiplier} phần.`}
            </span>
          </div>
        )}

        {/* Body Content */}
        {loading ? (
          <div className="flex min-h-48 flex-col items-center justify-center gap-3 text-slate-400">
            <Loader2 size={32} className="animate-spin text-indigo-600 dark:text-indigo-400" />
            <p className="text-sm font-semibold">
              {isEn ? 'Loading recipe & stock levels...' : 'Đang tải công thức và tồn kho...'}
            </p>
          </div>
        ) : error ? (
          <div className="rounded-2xl border border-rose-200 bg-rose-50 p-5 text-center dark:border-rose-500/30 dark:bg-rose-950/20">
            <p className="text-sm font-bold text-rose-700 dark:text-rose-300">{error}</p>
            <button
              type="button"
              onClick={loadRecipes}
              className="mt-3 rounded-xl bg-rose-600 px-4 py-1.5 text-xs font-bold text-white transition hover:bg-rose-500"
            >
              {isEn ? 'Retry' : 'Thử lại'}
            </button>
          </div>
        ) : target?.isCombo && target?.comboId ? (
          <div className="space-y-4">
            {scaledComboSections.length === 0 ? (
              <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50/70 px-4 py-8 text-center dark:border-slate-800 dark:bg-slate-900/40">
                <ChefHat size={26} className="mb-2 text-slate-400" />
                <p className="text-sm font-bold text-slate-600 dark:text-slate-300">
                  {isEn ? 'Combo has no dishes configured.' : 'Combo chưa có món thành phần.'}
                </p>
              </div>
            ) : (
              scaledComboSections.map((section) => (
                <div key={`${section.id || section.menuItemId}`} className="space-y-2">
                  <div className="flex flex-wrap items-center justify-between gap-2 px-1">
                    <div className="flex items-center gap-2">
                      <span className="inline-flex h-6 items-center rounded-lg bg-indigo-100 px-2 font-mono text-xs font-black text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
                        {section.quantity || 1} {isEn ? 'pc/combo' : 'phần/combo'}
                      </span>
                      <h4 className="text-sm font-black text-slate-800 dark:text-slate-100">
                        {section.menuItemName}
                      </h4>
                    </div>
                    <span className="font-mono text-xs font-bold text-slate-500 dark:text-slate-400">
                      {isEn
                        ? `Total needed: x${section.effectiveMultiplier}`
                        : `Tổng định lượng: x${section.effectiveMultiplier} phần`}
                    </span>
                  </div>
                  <RecipeTable
                    rows={section.scaledRows}
                    multiplier={section.effectiveMultiplier}
                    isEn={isEn}
                  />
                </div>
              ))
            )}
          </div>
        ) : (
          <RecipeTable rows={scaledSingleRecipes} multiplier={portionMultiplier} isEn={isEn} />
        )}
      </div>

      <div className="flex justify-end border-t border-slate-200/80 bg-slate-50/70 px-6 py-3.5 dark:border-slate-800 dark:bg-slate-900/80">
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

export default KitchenRecipeModal;
