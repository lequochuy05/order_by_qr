import { useState } from 'react';
import {
  AlertTriangle,
  ChevronDown,
  ChevronUp,
  Loader2,
  PackagePlus,
  Sparkles,
  TrendingUp,
} from 'lucide-react';

import { aiInventoryService } from '@entities/ai';
import { ProBadge } from '@entities/subscription';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { fmtVND } from '@shared/lib/formatters.js';
import { useTranslation } from '@shared/lib/i18n';
import { fmtQty } from '@features/inventory-management/lib/inventoryFormat.js';
import {
  FORECAST_DAY_OPTIONS,
  getUrgencyMeta,
} from '@features/inventory-management/lib/aiRestockHelpers.js';

const AiRestockPanel = ({ onQuickRestock, locked = false, onRequestUpgrade = null }) => {
  const { language, isEn } = useTranslation('inventory');
  const [expanded, setExpanded] = useState(false);
  const [forecastDays, setForecastDays] = useState(7);
  const [onlyUrgent, setOnlyUrgent] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  const handleAnalyze = async () => {
    if (locked) {
      onRequestUpgrade?.();
      return;
    }
    if (loading) return;
    setExpanded(true);
    setLoading(true);
    setError('');
    try {
      const response = await aiInventoryService.suggestRestock({ forecastDays, language });
      setResult(response);
    } catch (err) {
      setError(buildErrorMessage(err, { includeDetails: false }));
    } finally {
      setLoading(false);
    }
  };

  const allItems = result?.items || [];
  const visibleItems = onlyUrgent
    ? allItems.filter((item) => item.urgency !== 'OK' || Number(item.suggestedRestockQty) > 0)
    : allItems;

  return (
    <section
      aria-label={isEn ? 'AI Smart Restock Forecast' : 'AI Gợi ý nhập kho thông minh'}
      className="rounded-3xl border border-indigo-200/80 bg-gradient-to-br from-indigo-50/70 via-white to-violet-50/60 p-4 shadow-sm transition-colors sm:p-5 dark:border-indigo-500/25 dark:from-indigo-950/35 dark:via-slate-900 dark:to-violet-950/25"
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <button
          type="button"
          onClick={() => setExpanded((prev) => !prev)}
          className="flex min-w-0 items-center gap-3 text-left"
        >
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-500/25">
            <Sparkles size={18} />
          </div>
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-sm font-black text-slate-900 sm:text-base dark:text-white">
                {isEn ? 'AI Smart Restock Forecast' : 'AI Gợi ý nhập kho thông minh'}
              </h2>
              {locked && <ProBadge size="xs" />}
              {result && (
                <span className="rounded-full border border-indigo-200 bg-indigo-50 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-indigo-700 dark:border-indigo-500/30 dark:bg-indigo-500/15 dark:text-indigo-300">
                  {result.aiGenerated ? 'Gemini AI' : isEn ? 'Local Forecast' : 'Dự báo định mức'}
                </span>
              )}
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {isEn
                ? 'Combines 30-day menu velocity with BOM recipes (+10% safety buffer).'
                : 'Kết hợp tốc độ bán 30 ngày với định mức BOM từng món (+10% dự phòng).'}
            </p>
          </div>
          <span className="ml-1 text-slate-400 dark:text-slate-500">
            {expanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
          </span>
        </button>

        <div className="flex flex-wrap items-center gap-2">
          <div className="inline-flex rounded-xl border border-slate-200/80 bg-white p-1 dark:border-slate-800 dark:bg-slate-950">
            {FORECAST_DAY_OPTIONS.map((days) => (
              <button
                key={days}
                type="button"
                onClick={() => setForecastDays(days)}
                className={`rounded-lg px-2.5 py-1 text-xs font-bold transition ${
                  forecastDays === days
                    ? 'bg-indigo-600 text-white shadow-xs'
                    : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'
                }`}
              >
                {days} {isEn ? 'd' : 'ngày'}
              </button>
            ))}
          </div>

          <button
            type="button"
            onClick={handleAnalyze}
            disabled={loading}
            className="inline-flex h-9 items-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-3.5 text-xs font-black text-white shadow-md shadow-indigo-600/20 transition hover:from-indigo-500 hover:to-violet-500 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? <Loader2 size={14} className="animate-spin" /> : <TrendingUp size={14} />}
            {loading
              ? isEn
                ? 'Forecasting...'
                : 'Đang phân tích...'
              : result
                ? isEn
                  ? 'Refresh Forecast'
                  : 'Cập nhật dự báo'
                : isEn
                  ? 'Analyze Restock'
                  : 'Phân tích dự báo nhập kho'}
          </button>
        </div>
      </div>

      {expanded && (
        <div className="mt-4 space-y-4 border-t border-indigo-200/60 pt-4 dark:border-indigo-500/20">
          {loading && !result && (
            <div className="flex items-center justify-center gap-2 rounded-2xl border border-dashed border-indigo-200 bg-white/70 py-8 text-xs font-bold text-indigo-600 dark:border-indigo-500/25 dark:bg-slate-950/40 dark:text-indigo-300">
              <Loader2 size={16} className="animate-spin" />
              {isEn
                ? 'Calculating BOM consumption across menu orders...'
                : 'Đang tổng hợp tiêu hao nguyên liệu theo đơn bán...'}
            </div>
          )}

          {error && (
            <div className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs font-semibold text-rose-700 dark:border-rose-500/30 dark:bg-rose-950/40 dark:text-rose-300">
              <span className="inline-flex items-center gap-2">
                <AlertTriangle size={15} className="shrink-0" />
                {error}
              </span>
              <button
                type="button"
                onClick={handleAnalyze}
                className="rounded-lg border border-rose-300 bg-white px-2.5 py-1 font-bold text-rose-700 transition hover:bg-rose-100 dark:border-rose-500/40 dark:bg-rose-950 dark:text-rose-200"
              >
                {isEn ? 'Retry' : 'Thử lại'}
              </button>
            </div>
          )}

          {result && (
            <>
              <div className="grid grid-cols-1 gap-3 md:grid-cols-4">
                <div className="rounded-2xl border border-indigo-200/70 bg-white/90 p-3.5 md:col-span-2 dark:border-indigo-500/20 dark:bg-slate-950/60">
                  <p className="text-xs font-bold text-slate-800 dark:text-slate-100">
                    {result.summary}
                  </p>
                  {result.recommendations?.length > 0 && (
                    <ul className="mt-2 space-y-1 text-xs text-slate-600 dark:text-slate-300">
                      {result.recommendations.map((rec, idx) => (
                        <li key={idx} className="flex items-start gap-1.5">
                          <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-indigo-500" />
                          <span>{rec}</span>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>

                <div className="rounded-2xl border border-rose-200/70 bg-rose-50/70 p-3.5 dark:border-rose-500/25 dark:bg-rose-950/25">
                  <p className="text-[11px] font-bold uppercase tracking-wider text-rose-600 dark:text-rose-300">
                    {isEn ? 'Urgent / Shortfall' : 'Khẩn cấp / Thiếu hụt'}
                  </p>
                  <p className="mt-1 font-mono text-2xl font-black text-rose-700 dark:text-rose-200">
                    {result.criticalCount + result.highCount}
                  </p>
                  <p className="mt-0.5 text-[11px] font-semibold text-rose-600/80 dark:text-rose-300/80">
                    {result.criticalCount} {isEn ? 'critical' : 'khẩn cấp'} · {result.highCount}{' '}
                    {isEn ? 'shortfall' : 'thiếu hụt'}
                  </p>
                </div>

                <div className="rounded-2xl border border-slate-200/80 bg-white/90 p-3.5 dark:border-slate-800 dark:bg-slate-950/60">
                  <p className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    {isEn ? 'Est. Restock Budget' : 'Dự toán chi phí nhập'}
                  </p>
                  <p className="mt-1 font-mono text-xl font-black text-indigo-600 dark:text-indigo-400">
                    {fmtVND(result.totalEstimatedCost)}
                  </p>
                  <p className="mt-0.5 text-[11px] text-slate-500 dark:text-slate-400">
                    {isEn ? `Next ${result.forecastDays} days` : `Kỳ dự báo ${result.forecastDays} ngày`}
                  </p>
                </div>
              </div>

              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="text-xs font-bold text-slate-600 dark:text-slate-300">
                  {isEn
                    ? `Showing ${visibleItems.length} of ${allItems.length} ingredients`
                    : `Hiển thị ${visibleItems.length}/${allItems.length} nguyên liệu`}
                </p>
                <button
                  type="button"
                  onClick={() => setOnlyUrgent((prev) => !prev)}
                  className="rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-bold text-slate-700 transition hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
                >
                  {onlyUrgent
                    ? isEn
                      ? 'Show all items'
                      : 'Hiện cả nguyên liệu đủ dùng'
                    : isEn
                      ? 'Need restock only'
                      : 'Chỉ hiện mục cần nhập'}
                </button>
              </div>

              {visibleItems.length === 0 ? (
                <div className="rounded-2xl border border-dashed border-emerald-200 bg-emerald-50/50 py-6 text-center text-xs font-bold text-emerald-700 dark:border-emerald-500/25 dark:bg-emerald-950/20 dark:text-emerald-300">
                  {isEn
                    ? 'All active inventory items meet projected demand for this window.'
                    : 'Tồn kho hiện tại đáp ứng đủ dự báo tiêu thụ cho giai đoạn này.'}
                </div>
              ) : (
                <div className="max-h-96 space-y-2.5 overflow-y-auto pr-1">
                  {visibleItems.map((item) => {
                    const meta = getUrgencyMeta(item.urgency, language);
                    return (
                      <div
                        key={item.inventoryItemId}
                        className={`flex flex-wrap items-center justify-between gap-3 rounded-2xl border p-3.5 transition ${meta.rowClass}`}
                      >
                        <div className="min-w-0 flex-1 space-y-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className={`rounded-lg border px-2 py-0.5 text-[11px] font-black ${meta.badgeClass}`}>
                              {meta.label}
                            </span>
                            <h3 className="text-sm font-black text-slate-900 dark:text-white">
                              {item.name}
                            </h3>
                            {item.daysUntilStockout !== null && item.daysUntilStockout !== undefined && (
                              <span className="rounded-md bg-slate-100 px-2 py-0.5 font-mono text-[11px] font-bold text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                                {isEn ? `~${item.daysUntilStockout}d left` : `Còn ~${item.daysUntilStockout} ngày`}
                              </span>
                            )}
                          </div>

                          <p className="text-xs text-slate-600 dark:text-slate-300">{item.reason}</p>

                          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px] text-slate-500 dark:text-slate-400">
                            <span>
                              {isEn ? 'Available' : 'Khả dụng'}:{' '}
                              <strong className="font-mono text-slate-800 dark:text-slate-200">
                                {fmtQty(item.availableQuantity)} {item.unit}
                              </strong>
                            </span>
                            <span>
                              {isEn ? 'Projected' : 'Dự báo cần'}:{' '}
                              <strong className="font-mono text-slate-800 dark:text-slate-200">
                                {fmtQty(item.projectedUsage)} {item.unit}
                              </strong>
                            </span>
                            {Number(item.unitCost) > 0 && (
                              <span>
                                {isEn ? 'Est. cost' : 'Ước chi'}:{' '}
                                <strong className="font-mono text-indigo-600 dark:text-indigo-400">
                                  {fmtVND(item.estimatedCost)}
                                </strong>
                              </span>
                            )}
                            {item.drivenByDishes?.length > 0 && (
                              <span className="truncate">
                                {isEn ? 'Driven by' : 'Món tiêu thụ'}:{' '}
                                <strong className="text-slate-700 dark:text-slate-300">
                                  {item.drivenByDishes.join(', ')}
                                </strong>
                              </span>
                            )}
                          </div>
                        </div>

                        {onQuickRestock && (
                          <button
                            type="button"
                            onClick={() => onQuickRestock(item, language)}
                            className="inline-flex h-9 shrink-0 items-center gap-1.5 rounded-xl border border-indigo-200 bg-indigo-50 px-3 text-xs font-black text-indigo-700 transition hover:bg-indigo-600 hover:text-white active:scale-95 dark:border-indigo-500/30 dark:bg-indigo-500/15 dark:text-indigo-300 dark:hover:bg-indigo-600 dark:hover:text-white"
                          >
                            <PackagePlus size={14} />
                            {Number(item.suggestedRestockQty) > 0
                              ? `+${fmtQty(item.suggestedRestockQty)} ${item.unit}`
                              : isEn
                                ? 'Restock'
                                : 'Nhập nhanh'}
                          </button>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </>
          )}
        </div>
      )}
    </section>
  );
};

export default AiRestockPanel;
