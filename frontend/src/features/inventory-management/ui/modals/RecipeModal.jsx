import { useMemo, useState } from 'react';
import {
  AlertTriangle,
  ChefHat,
  CheckCircle2,
  ChevronDown,
  Loader2,
  Lock,
  Plus,
  Sparkles,
  Trash2,
} from 'lucide-react';

import { aiInventoryService } from '@shared/api/aiService.js';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { fmtVND } from '@shared/lib/formatters.js';
import { useTranslation } from '@shared/lib/i18n';
import {
  buildDraftRecipePayloadLines,
  calculateLocalBomAnalysis,
  getMarginStatusMeta,
  mapSuggestedItemsToRecipeRows,
} from '@features/inventory-management/lib/bomCostHelpers.js';
import { fmtQty } from '@features/inventory-management/lib/inventoryFormat.js';
import {
  FormLabel,
  ModalActions,
  ModalHeader,
  SelectField,
  SharedModal,
  TextField,
} from '@shared/ui';

const RecipeModal = ({
  menuItems,
  inventoryItems,
  selectedMenuItem,
  selectedMenuItemId,
  onSelectMenuItem,
  recipeItems,
  setRecipeItems,
  onAddRow,
  onClose,
  onSubmit,
  isSubmitting = false,
  loading = false,
  isManager = true,
}) => {
  const { language, isEn } = useTranslation('inventory');
  const [aiState, setAiState] = useState({
    menuItemId: null,
    loading: false,
    error: '',
    data: null,
    applied: false,
  });

  const isCurrentAiMatch = String(aiState.menuItemId) === String(selectedMenuItemId);
  const activeAiData = isCurrentAiMatch ? aiState.data : null;
  const activeAiError = isCurrentAiMatch ? aiState.error : '';
  const activeAiApplied = isCurrentAiMatch ? aiState.applied : false;

  const localAnalysis = useMemo(
    () => calculateLocalBomAnalysis(selectedMenuItem?.price, recipeItems, inventoryItems),
    [selectedMenuItem?.price, recipeItems, inventoryItems],
  );
  const marginMeta = getMarginStatusMeta(localAnalysis.marginStatus, language);

  const handleAnalyzeBom = async () => {
    if (!selectedMenuItemId || aiState.loading) return;
    const targetMenuId = selectedMenuItemId;
    setAiState({
      menuItemId: targetMenuId,
      loading: true,
      error: '',
      data: null,
      applied: false,
    });
    try {
      const data = await aiInventoryService.assistRecipe({
        menuItemId: Number(targetMenuId),
        currentItems: buildDraftRecipePayloadLines(recipeItems),
        language,
      });
      setAiState({
        menuItemId: targetMenuId,
        loading: false,
        error: '',
        data,
        applied: false,
      });
    } catch (err) {
      setAiState({
        menuItemId: targetMenuId,
        loading: false,
        error: buildErrorMessage(err, { includeDetails: false }),
        data: null,
        applied: false,
      });
    }
  };

  const handleApplyAiBom = () => {
    if (!isManager || !activeAiData?.suggestedItems?.length) return;
    setRecipeItems(mapSuggestedItemsToRecipeRows(activeAiData.suggestedItems));
    setAiState((prev) => ({ ...prev, applied: true }));
  };

  const costRatioWidth =
    localAnalysis.foodCostPercent !== null
      ? Math.min(100, Math.max(4, localAnalysis.foodCostPercent))
      : 0;

  return (
    <SharedModal isOpen onClose={onClose} className="max-w-3xl !p-0" ariaLabel="Định mức nguyên liệu">
      <ModalHeader
        title="Định mức nguyên liệu"
        subtitle={selectedMenuItem?.name || 'Định mức nguyên liệu tiêu hao cho mỗi phần'}
        icon={ChefHat}
        onClose={onClose}
        disabled={isSubmitting}
      />

      <form
        id="recipeForm"
        onSubmit={(e) => {
          if (!isManager) {
            e.preventDefault();
            return;
          }
          onSubmit(e);
        }}
        className="custom-scrollbar flex-1 space-y-5 overflow-y-auto p-6 sm:p-8"
      >
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div className="min-w-[220px] flex-1">
            <SelectField
              label="Món ăn"
              value={selectedMenuItemId}
              onChange={onSelectMenuItem}
              options={menuItems.map((item) => ({ value: item.id, label: item.name }))}
              selectClassName="font-bold"
            />
          </div>

          <button
            type="button"
            onClick={handleAnalyzeBom}
            disabled={loading || aiState.loading || !selectedMenuItemId}
            className="inline-flex h-[50px] shrink-0 items-center gap-2 rounded-2xl bg-gradient-to-r from-indigo-600 to-violet-600 px-4 text-xs font-black text-white shadow-md shadow-indigo-600/20 transition hover:from-indigo-500 hover:to-violet-500 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
          >
            {aiState.loading ? (
              <Loader2 size={15} className="animate-spin" />
            ) : (
              <Sparkles size={15} />
            )}
            {aiState.loading
              ? isEn
                ? 'Analyzing BOM...'
                : 'Đang phân tích...'
              : isEn
                ? 'AI BOM & Food Cost'
                : 'AI Gợi ý định mức & Giá vốn'}
          </button>
        </div>

        {/* Live Food Cost & Margin Summary */}
        <div className="rounded-2xl border border-slate-200/80 bg-slate-50/80 p-4 dark:border-slate-800 dark:bg-slate-900/70">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <span className="text-xs font-black uppercase tracking-wider text-slate-500 dark:text-slate-400">
              {isEn ? 'Live Food Cost Check' : 'Kiểm tra giá vốn món'}
            </span>
            <span className={`rounded-lg border px-2.5 py-0.5 text-[11px] font-bold ${marginMeta.badgeClass}`}>
              {marginMeta.label}
            </span>
          </div>

          <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                {isEn ? 'Menu Price' : 'Giá bán'}
              </p>
              <p className="font-mono text-sm font-black text-slate-900 dark:text-white">
                {fmtVND(selectedMenuItem?.price || 0)}
              </p>
            </div>
            <div>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                {isEn ? 'Est. Food Cost' : 'Giá vốn ước tính'}
              </p>
              <p className="font-mono text-sm font-black text-indigo-600 dark:text-indigo-400">
                {fmtVND(localAnalysis.totalFoodCost)}
              </p>
            </div>
            <div>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                {isEn ? 'Cost Ratio' : 'Tỷ lệ giá vốn'}
              </p>
              <p className="font-mono text-sm font-black text-slate-900 dark:text-white">
                {localAnalysis.foodCostPercent !== null ? `${localAnalysis.foodCostPercent}%` : '—'}
              </p>
            </div>
            <div>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                {isEn ? 'Gross Profit' : 'Lãi gộp / phần'}
              </p>
              <p
                className={`font-mono text-sm font-black ${
                  localAnalysis.grossProfit < 0
                    ? 'text-rose-600 dark:text-rose-400'
                    : 'text-emerald-600 dark:text-emerald-400'
                }`}
              >
                {fmtVND(localAnalysis.grossProfit)}
              </p>
            </div>
          </div>

          {localAnalysis.foodCostPercent !== null && (
            <div className="mt-3 h-1.5 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
              <div
                className={`h-full rounded-full transition-all duration-300 ${marginMeta.barClass}`}
                style={{ width: `${costRatioWidth}%` }}
              />
            </div>
          )}

          {localAnalysis.missingUnitCostCount > 0 && (
            <p className="mt-2 text-[11px] font-semibold text-amber-700 dark:text-amber-300">
              {isEn
                ? `${localAnalysis.missingUnitCostCount} ingredient(s) have unit cost 0 — update in Inventory for accurate margin.`
                : `${localAnalysis.missingUnitCostCount} nguyên liệu đang để đơn giá 0đ — hãy cập nhật giá nhập kho để tính lãi gộp chính xác.`}
            </p>
          )}
          {localAnalysis.recommendedMinPrice > Number(selectedMenuItem?.price || 0) &&
            localAnalysis.totalFoodCost > 0 && (
              <p className="mt-1.5 text-[11px] font-semibold text-rose-600 dark:text-rose-300">
                {isEn
                  ? `Recommended min price (<=35% food cost): ${fmtVND(localAnalysis.recommendedMinPrice)}`
                  : `Giá bán tối thiểu gợi ý (giữ giá vốn <= 35%): ${fmtVND(localAnalysis.recommendedMinPrice)}`}
              </p>
            )}
        </div>

        {/* AI BOM & Cost Assistant Results */}
        {activeAiError && (
          <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs font-semibold text-rose-700 dark:border-rose-500/30 dark:bg-rose-950/40 dark:text-rose-300">
            <AlertTriangle size={14} className="mr-1.5 inline" />
            {activeAiError}
          </div>
        )}

        {activeAiData && (
          <div className="space-y-3 rounded-2xl border border-indigo-200/80 bg-indigo-50/50 p-4 dark:border-indigo-500/30 dark:bg-indigo-950/25">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <Sparkles size={15} className="text-indigo-600 dark:text-indigo-400" />
                <span className="text-xs font-black text-slate-900 dark:text-white">
                  {isEn ? 'AI Culinary & Cost Guidance' : 'Tư vấn định mức & Giá vốn AI'}
                </span>
                <span className="rounded-full border border-indigo-200 bg-white px-2 py-0.5 text-[10px] font-bold text-indigo-700 dark:border-indigo-500/30 dark:bg-slate-900 dark:text-indigo-300">
                  {activeAiData.aiGenerated ? 'Gemini AI' : isEn ? 'Local Rules' : 'Phân tích nội bộ'}
                </span>
              </div>

              {isManager && activeAiData.suggestedItems?.length > 0 && (
                <button
                  type="button"
                  onClick={handleApplyAiBom}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3 py-1.5 text-xs font-black text-white shadow-xs transition hover:bg-indigo-500 active:scale-95"
                >
                  {activeAiApplied ? <CheckCircle2 size={14} /> : <Sparkles size={14} />}
                  {activeAiApplied
                    ? isEn
                      ? 'Applied to Form'
                      : 'Đã điền vào công thức'
                    : isEn
                      ? 'Apply AI Recipe'
                      : 'Áp dụng công thức AI'}
                </button>
              )}
            </div>

            {activeAiData.costWarnings?.length > 0 && (
              <div className="space-y-1 rounded-xl border border-amber-200/80 bg-amber-50/80 p-3 text-xs text-amber-900 dark:border-amber-500/30 dark:bg-amber-950/30 dark:text-amber-200">
                {activeAiData.costWarnings.map((warning, idx) => (
                  <p key={idx} className="flex items-start gap-1.5 font-semibold">
                    <AlertTriangle size={13} className="mt-0.5 shrink-0" />
                    <span>{warning}</span>
                  </p>
                ))}
              </div>
            )}

            {activeAiData.pricingAdvice?.length > 0 && (
              <ul className="space-y-1 text-xs text-slate-700 dark:text-slate-300">
                {activeAiData.pricingAdvice.map((tip, idx) => (
                  <li key={idx} className="flex items-start gap-1.5">
                    <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-indigo-500" />
                    <span>{tip}</span>
                  </li>
                ))}
              </ul>
            )}

            {activeAiData.suggestedItems?.length > 0 && (
              <div className="space-y-1.5 rounded-xl border border-indigo-100 bg-white/90 p-3 dark:border-indigo-500/20 dark:bg-slate-950/60">
                <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] font-bold text-slate-500 dark:text-slate-400">
                  <span>{isEn ? 'Suggested BOM Lines' : 'Định mức AI đề xuất'}</span>
                  {activeAiData.suggestedAnalysis && (
                    <span className="font-mono text-indigo-600 dark:text-indigo-400">
                      {fmtVND(activeAiData.suggestedAnalysis.totalFoodCost)} (
                      {activeAiData.suggestedAnalysis.foodCostPercent ?? '—'}%)
                    </span>
                  )}
                </div>
                <div className="divide-y divide-slate-100 dark:divide-slate-800/80">
                  {activeAiData.suggestedItems.map((line) => (
                    <div
                      key={line.inventoryItemId}
                      className="flex flex-wrap items-center justify-between gap-2 py-1.5 text-xs"
                    >
                      <div className="min-w-0">
                        <span className="font-bold text-slate-900 dark:text-slate-100">
                          {line.inventoryItemName}
                        </span>
                        {line.reason && (
                          <span className="ml-2 text-[11px] text-slate-500 dark:text-slate-400">
                            — {line.reason}
                          </span>
                        )}
                      </div>
                      <span className="shrink-0 font-mono font-bold text-slate-700 dark:text-slate-200">
                        {fmtQty(line.quantityRequired)} {line.unit} · {fmtVND(line.lineCost)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {!isManager && (
          <div className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-xs font-semibold text-slate-600 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300">
            <Lock size={14} className="shrink-0 text-slate-400" />
            {isEn
              ? 'Read-only Chef view: only Managers can apply changes or save BOM recipes.'
              : 'Chế độ xem Bếp: chỉ Quản lý mới được áp dụng và lưu thay đổi định mức.'}
          </div>
        )}

        {loading ? (
          <div className="flex justify-center rounded-2xl border border-dashed border-slate-200/80 py-12 text-indigo-600 dark:border-slate-800/80 dark:text-indigo-400">
            <Loader2 size={28} className="animate-spin" />
          </div>
        ) : (
          <div className="space-y-3">
            {recipeItems.map((row, index) => {
              const selectedInventory = inventoryItems.find(
                (item) => String(item.id) === String(row.inventoryItemId),
              );
              return (
                <div
                  key={`${row.inventoryItemId}-${index}`}
                  className={`grid grid-cols-1 gap-3 rounded-2xl border border-slate-200/80 bg-slate-50/70 p-4 transition-colors dark:border-slate-800/80 dark:bg-slate-900/60 ${
                    isManager ? 'md:grid-cols-[1fr_190px_48px]' : 'md:grid-cols-[1fr_190px]'
                  } md:items-end`}
                >
                  <label className="block">
                    <FormLabel className="!mb-2 !tracking-[0.16em]">Nguyên liệu</FormLabel>
                    <span className="group relative block">
                      <select
                        value={row.inventoryItemId}
                        disabled={!isManager}
                        onChange={(e) => {
                          const next = [...recipeItems];
                          next[index] = { ...row, inventoryItemId: e.target.value };
                          setRecipeItems(next);
                        }}
                        className="h-[50px] w-full cursor-pointer appearance-none rounded-2xl border-2 border-slate-200/75 bg-white px-4 pr-10 text-sm font-bold text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10 disabled:cursor-not-allowed disabled:opacity-70 dark:border-slate-800/90 dark:bg-slate-950 dark:text-slate-100"
                      >
                        {inventoryItems.map((item) => (
                          <option key={item.id} value={item.id}>
                            {item.name} ({item.unit})
                          </option>
                        ))}
                      </select>
                      <ChevronDown
                        size={16}
                        className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-500 dark:text-slate-500"
                      />
                    </span>
                  </label>
                  <TextField
                    type="number"
                    min="0.001"
                    step="0.001"
                    label={`Lượng / món${selectedInventory ? ` (${selectedInventory.unit})` : ''}`}
                    value={row.quantityRequired}
                    disabled={!isManager}
                    onChange={(value) => {
                      const next = [...recipeItems];
                      next[index] = { ...row, quantityRequired: value };
                      setRecipeItems(next);
                    }}
                    required
                  />
                  {isManager && (
                    <button
                      type="button"
                      title="Xóa dòng"
                      aria-label="Xóa dòng nguyên liệu"
                      onClick={() =>
                        setRecipeItems(recipeItems.filter((_, rowIndex) => rowIndex !== index))
                      }
                      className="flex h-[50px] items-center justify-center rounded-2xl border border-rose-200/80 bg-rose-50 text-rose-600 transition-all hover:border-rose-500 hover:bg-rose-600 hover:text-white active:scale-95 dark:border-rose-800/40 dark:bg-rose-950/40 dark:text-rose-400 dark:hover:bg-rose-600 dark:hover:text-white"
                    >
                      <Trash2 size={17} />
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {!loading && recipeItems.length === 0 && (
          <div className="rounded-2xl border-2 border-dashed border-slate-200/80 py-10 text-center text-sm font-medium italic text-slate-400 dark:border-slate-800/80 dark:text-slate-500">
            Chưa có nguyên liệu trong định mức món này.
          </div>
        )}

        {isManager && (
          <button
            type="button"
            onClick={onAddRow}
            disabled={loading || isSubmitting}
            className="inline-flex h-10 items-center gap-2 rounded-xl border border-indigo-200/80 bg-indigo-50/90 px-4 text-xs font-bold text-indigo-700 transition-all hover:bg-indigo-600 hover:text-white active:scale-95 disabled:cursor-not-allowed disabled:opacity-50 dark:border-indigo-800/50 dark:bg-indigo-950/40 dark:text-indigo-300 dark:hover:bg-indigo-600 dark:hover:text-white"
          >
            <Plus size={15} /> Thêm nguyên liệu
          </button>
        )}
      </form>

      {isManager ? (
        <ModalActions
          formId="recipeForm"
          submitLabel="Lưu định mức"
          onClose={onClose}
          isSubmitting={isSubmitting}
        />
      ) : (
        <div className="flex shrink-0 items-center justify-end border-t border-slate-200/70 bg-slate-50/85 px-6 py-4 dark:border-slate-800/80 dark:bg-slate-900/95 sm:px-8">
          <button
            type="button"
            onClick={onClose}
            className="inline-flex h-11 items-center justify-center rounded-xl border border-slate-200/90 bg-white px-6 text-sm font-bold text-slate-700 transition hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
          >
            {isEn ? 'Close' : 'Đóng'}
          </button>
        </div>
      )}
    </SharedModal>
  );
};

export default RecipeModal;
