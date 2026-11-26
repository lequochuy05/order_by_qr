import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import {
  AlertTriangle,
  Bot,
  CheckCircle2,
  Lightbulb,
  Loader2,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  TrendingUp,
} from 'lucide-react';

import { analyticsService } from '@entities/analytics';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { fmtTime, fmtVND } from '@shared/lib/formatters.js';
import { useTranslation } from '@shared/lib/i18n';
import {
  computeInsightsDateRange,
  formatDeltaPercent,
  getSeverityMeta,
  normalizeExecutiveSummary,
} from '../lib/executiveInsights.js';

const PRESET_OPTIONS = [
  { id: 'today', vi: 'Hôm nay', en: 'Today' },
  { id: '7d', vi: '7 ngày', en: '7 days' },
  { id: '30d', vi: '30 ngày', en: '30 days' },
];

const AiExecutiveSummaryCard = ({ businessToday }) => {
  const { isEn, language } = useTranslation('dashboard');
  const [preset, setPreset] = useState('7d');
  const [summary, setSummary] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');

  const mutation = useMutation({
    mutationFn: ({ targetPreset, targetLanguage }) => {
      const range = computeInsightsDateRange(businessToday, targetPreset);
      return analyticsService.getAiExecutiveSummary({
        from: range.from,
        to: range.to,
        language: targetLanguage,
      });
    },
    onSuccess: (rawData) => {
      setErrorMessage('');
      setSummary(normalizeExecutiveSummary(rawData));
    },
    onError: (err) => {
      setErrorMessage(
        buildErrorMessage(err, { includeDetails: false }) ||
          (isEn
            ? 'Unable to generate executive summary right now.'
            : 'Không thể tạo tóm tắt điều hành lúc này.'),
      );
    },
  });

  const handleAnalyze = (nextPreset = preset) => {
    setErrorMessage('');
    mutation.mutate({ targetPreset: nextPreset, targetLanguage: language });
  };

  const handleSelectPreset = (nextPreset) => {
    setPreset(nextPreset);
    if (summary && !mutation.isPending) {
      handleAnalyze(nextPreset);
    }
  };

  const deltaText = summary ? formatDeltaPercent(summary.metrics.revenueChangePercent) : null;
  const activeRange = computeInsightsDateRange(businessToday, preset);

  return (
    <section
      aria-label={isEn ? 'AI Executive Summary' : 'AI Tóm tắt điều hành'}
      className="min-w-0 relative rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm transition-all duration-300"
    >
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2.5">
            <span className="p-2 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/20">
              <Sparkles size={18} />
            </span>
            <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
              {isEn ? 'AI Shift & Executive Briefing' : 'AI Tóm tắt ca & Phân tích điều hành'}
            </h3>
            {summary && (
              <span
                className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[11px] font-bold ${
                  summary.aiGenerated
                    ? 'border-indigo-300/80 bg-indigo-50 text-indigo-700 dark:border-indigo-500/40 dark:bg-indigo-950/60 dark:text-indigo-300'
                    : 'border-slate-300 bg-slate-100 text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300'
                }`}
              >
                <Bot size={12} />
                {summary.aiGenerated
                  ? isEn
                    ? 'AI Gemini'
                    : 'Phân tích AI'
                  : isEn
                    ? 'Auto Rule Summary'
                    : 'Tổng hợp tự động'}
              </span>
            )}
          </div>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            {isEn
              ? `Instant diagnosis of revenue delta, cancellations, kitchen bottlenecks (>20m), table SLA (>5m) & inventory alerts (${activeRange.from} → ${activeRange.to}).`
              : `Đánh giá nhanh doanh thu so kỳ trước, đơn hủy, nghẽn bếp (>20p), phục vụ bàn (>5p) & cảnh báo kho (${activeRange.from} → ${activeRange.to}).`}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <div
            role="group"
            aria-label={isEn ? 'Summary period' : 'Khung thời gian phân tích'}
            className="inline-flex rounded-xl border border-slate-200 bg-slate-100/80 p-1 dark:border-slate-800 dark:bg-slate-900"
          >
            {PRESET_OPTIONS.map((option) => {
              const active = preset === option.id;
              return (
                <button
                  key={option.id}
                  type="button"
                  disabled={mutation.isPending}
                  onClick={() => handleSelectPreset(option.id)}
                  className={`rounded-lg px-3 py-1.5 text-xs font-bold transition-all ${
                    active
                      ? 'bg-white text-indigo-600 shadow-xs dark:bg-slate-800 dark:text-indigo-400'
                      : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200'
                  }`}
                >
                  {isEn ? option.en : option.vi}
                </button>
              );
            })}
          </div>

          <button
            type="button"
            disabled={mutation.isPending}
            onClick={() => handleAnalyze(preset)}
            className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-4 py-2 text-xs font-extrabold text-white shadow-md shadow-indigo-600/25 transition-all hover:from-indigo-500 hover:to-violet-500 active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {mutation.isPending ? (
              <Loader2 size={14} className="animate-spin" />
            ) : summary ? (
              <RefreshCw size={14} />
            ) : (
              <Sparkles size={14} />
            )}
            <span>
              {mutation.isPending
                ? isEn
                  ? 'Analyzing...'
                  : 'Đang phân tích...'
                : summary
                  ? isEn
                    ? 'Refresh briefing'
                    : 'Phân tích lại'
                  : isEn
                    ? 'Quick AI analysis'
                    : 'Phân tích nhanh bằng AI'}
            </span>
          </button>
        </div>
      </div>

      {/* Loading skeleton */}
      {mutation.isPending && (
        <div className="mt-5 space-y-3 animate-pulse">
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
            {[0, 1, 2, 3, 4].map((idx) => (
              <div
                key={idx}
                className="h-16 rounded-2xl bg-slate-100 dark:bg-slate-800/70"
              />
            ))}
          </div>
          <div className="h-12 rounded-2xl bg-slate-100 dark:bg-slate-800/70" />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div className="h-28 rounded-2xl bg-slate-100 dark:bg-slate-800/70" />
            <div className="h-28 rounded-2xl bg-slate-100 dark:bg-slate-800/70" />
          </div>
        </div>
      )}

      {/* Error state */}
      {!mutation.isPending && errorMessage && (
        <div className="mt-5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-2xl border border-rose-200 bg-rose-50/80 p-4 text-xs text-rose-800 dark:border-rose-500/40 dark:bg-rose-950/35 dark:text-rose-300">
          <div className="flex items-start gap-2.5">
            <AlertTriangle size={16} className="shrink-0 mt-0.5" />
            <span className="font-semibold">{errorMessage}</span>
          </div>
          <button
            type="button"
            onClick={() => handleAnalyze(preset)}
            className="inline-flex shrink-0 items-center gap-1.5 rounded-xl bg-rose-600 px-3 py-1.5 text-xs font-bold text-white transition-colors hover:bg-rose-500"
          >
            <RefreshCw size={12} />
            <span>{isEn ? 'Retry' : 'Thử lại'}</span>
          </button>
        </div>
      )}

      {/* Idle prompt state */}
      {!mutation.isPending && !errorMessage && !summary && (
        <div className="mt-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 rounded-2xl border border-dashed border-slate-300 bg-slate-50/70 p-4 dark:border-slate-700 dark:bg-slate-900/50">
          <div className="flex items-start gap-3">
            <TrendingUp size={18} className="mt-0.5 shrink-0 text-indigo-600 dark:text-indigo-400" />
            <div className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
              <span className="font-bold text-slate-900 dark:text-white">
                {isEn ? 'On-demand executive report: ' : 'Báo cáo điều hành theo yêu cầu: '}
              </span>
              {isEn
                ? 'Select Today, 7 days or 30 days and click "Quick AI analysis" to review real KPI deltas, delays and recommended actions.'
                : 'Chọn khung Hôm nay, 7 ngày hoặc 30 ngày rồi bấm "Phân tích nhanh bằng AI" để nhận đánh giá điểm nhấn, cảnh báo trễ hạn và hành động ưu tiên.'}
            </div>
          </div>
        </div>
      )}

      {/* Populated state */}
      {!mutation.isPending && summary && (
        <div className="mt-5 space-y-5">
          {/* Operational Pulse Metrics Bar */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
            <div className="rounded-2xl border border-slate-200/80 bg-slate-50/80 p-3 dark:border-slate-800 dark:bg-slate-900/60">
              <p className="text-[10px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                {isEn ? 'Period revenue' : 'Doanh thu kỳ'}
              </p>
              <p className="mt-1 font-mono text-sm font-black tabular-nums text-slate-900 dark:text-white">
                {fmtVND(summary.metrics.revenue)}
              </p>
              {deltaText && (
                <p
                  className={`mt-0.5 font-mono text-[11px] font-bold ${
                    summary.metrics.revenueChangePercent >= 0
                      ? 'text-emerald-600 dark:text-emerald-400'
                      : 'text-rose-600 dark:text-rose-400'
                  }`}
                >
                  {deltaText} {isEn ? 'vs prev' : 'so kỳ trước'}
                </p>
              )}
            </div>

            <div className="rounded-2xl border border-slate-200/80 bg-slate-50/80 p-3 dark:border-slate-800 dark:bg-slate-900/60">
              <p className="text-[10px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                {isEn ? 'Orders (Done / Cancel)' : 'Đơn (Xong / Hủy)'}
              </p>
              <p className="mt-1 font-mono text-sm font-black tabular-nums text-slate-900 dark:text-white">
                {summary.metrics.completedOrders}{' '}
                <span className="text-slate-400 dark:text-slate-500">/</span>{' '}
                <span
                  className={
                    summary.metrics.cancelledOrders > 0
                      ? 'text-rose-600 dark:text-rose-400'
                      : 'text-slate-600 dark:text-slate-300'
                  }
                >
                  {summary.metrics.cancelledOrders}
                </span>
              </p>
              <p className="mt-0.5 font-mono text-[11px] text-slate-500 dark:text-slate-400">
                AOV: {fmtVND(summary.metrics.averageOrderValue)}
              </p>
            </div>

            <div className="rounded-2xl border border-slate-200/80 bg-slate-50/80 p-3 dark:border-slate-800 dark:bg-slate-900/60">
              <p className="text-[10px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                {isEn ? 'Kitchen (>20m late)' : 'Bếp (Chờ / Trễ >20p)'}
              </p>
              <p className="mt-1 font-mono text-sm font-black tabular-nums text-slate-900 dark:text-white">
                {summary.metrics.activeKitchenItems}{' '}
                <span className="text-slate-400 dark:text-slate-500">/</span>{' '}
                <span
                  className={
                    summary.metrics.overdueKitchenItems > 0
                      ? 'text-rose-600 dark:text-rose-400'
                      : 'text-emerald-600 dark:text-emerald-400'
                  }
                >
                  {summary.metrics.overdueKitchenItems}
                </span>
              </p>
              <p className="mt-0.5 text-[11px] text-slate-500 dark:text-slate-400">
                {isEn ? 'Max wait:' : 'Lâu nhất:'} {summary.metrics.maxKitchenWaitMinutes}m
              </p>
            </div>

            <div className="rounded-2xl border border-slate-200/80 bg-slate-50/80 p-3 dark:border-slate-800 dark:bg-slate-900/60">
              <p className="text-[10px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                {isEn ? 'Service SLA (>5m)' : 'Yêu cầu bàn (>5p)'}
              </p>
              <p className="mt-1 font-mono text-sm font-black tabular-nums text-slate-900 dark:text-white">
                {summary.metrics.totalServiceRequests}{' '}
                <span className="text-slate-400 dark:text-slate-500">/</span>{' '}
                <span
                  className={
                    summary.metrics.overdueServiceRequests > 0
                      ? 'text-rose-600 dark:text-rose-400'
                      : 'text-emerald-600 dark:text-emerald-400'
                  }
                >
                  {summary.metrics.overdueServiceRequests}
                </span>
              </p>
              <p className="mt-0.5 text-[11px] text-slate-500 dark:text-slate-400">
                {isEn ? 'Open:' : 'Đang mở:'} {summary.metrics.openServiceRequests}
              </p>
            </div>

            <div className="col-span-2 sm:col-span-1 rounded-2xl border border-slate-200/80 bg-slate-50/80 p-3 dark:border-slate-800 dark:bg-slate-900/60">
              <p className="text-[10px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                {isEn ? 'Low / Out stock' : 'Kho (Thấp / Hết)'}
              </p>
              <p className="mt-1 font-mono text-sm font-black tabular-nums text-slate-900 dark:text-white">
                <span
                  className={
                    summary.metrics.lowStockItems > 0
                      ? 'text-amber-600 dark:text-amber-400'
                      : 'text-emerald-600 dark:text-emerald-400'
                  }
                >
                  {summary.metrics.lowStockItems}
                </span>{' '}
                <span className="text-slate-400 dark:text-slate-500">/</span>{' '}
                <span
                  className={
                    summary.metrics.outOfStockItems > 0
                      ? 'text-rose-600 dark:text-rose-400'
                      : 'text-slate-600 dark:text-slate-300'
                  }
                >
                  {summary.metrics.outOfStockItems}
                </span>
              </p>
              <p className="mt-0.5 truncate text-[11px] text-slate-500 dark:text-slate-400">
                {summary.metrics.lowStockItemNames[0] ||
                  (isEn ? 'Inventory healthy' : 'Tồn kho ổn định')}
              </p>
            </div>
          </div>

          {/* Headline Briefing Box */}
          <div className="rounded-2xl border border-indigo-200/80 bg-gradient-to-r from-indigo-50/90 via-violet-50/60 to-purple-50/40 p-4 dark:border-indigo-500/30 dark:from-indigo-950/50 dark:via-violet-950/35 dark:to-purple-950/25">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm font-extrabold text-slate-900 dark:text-white">
                {summary.headline}
              </p>
              {summary.generatedAt && (
                <span className="font-mono text-[11px] font-semibold text-slate-500 dark:text-slate-400">
                  {fmtTime(summary.generatedAt)}
                </span>
              )}
            </div>
          </div>

          {/* Highlights & Recommendations + Anomalies */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {/* Highlights & Recommendations */}
            <div className="space-y-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 p-4 dark:border-slate-800 dark:bg-slate-900/40">
              {summary.highlights.length > 0 && (
                <div>
                  <h4 className="mb-2 flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-wider text-slate-700 dark:text-slate-200">
                    <CheckCircle2 size={14} className="text-emerald-600 dark:text-emerald-400" />
                    <span>{isEn ? 'Key highlights' : 'Điểm nhấn chính'}</span>
                  </h4>
                  <ul className="space-y-1.5">
                    {summary.highlights.map((item, idx) => (
                      <li
                        key={idx}
                        className="flex items-start gap-2 text-xs leading-relaxed text-slate-700 dark:text-slate-300"
                      >
                        <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-indigo-500" />
                        <span>{item}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {summary.recommendations.length > 0 && (
                <div className="border-t border-slate-200/80 pt-3 dark:border-slate-800">
                  <h4 className="mb-2 flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-wider text-indigo-700 dark:text-indigo-300">
                    <Lightbulb size={14} className="text-indigo-600 dark:text-indigo-400" />
                    <span>{isEn ? 'Recommended actions' : 'Khuyến nghị hành động'}</span>
                  </h4>
                  <ul className="space-y-1.5">
                    {summary.recommendations.map((rec, idx) => (
                      <li
                        key={idx}
                        className="flex items-start gap-2 text-xs leading-relaxed text-slate-700 dark:text-slate-300"
                      >
                        <span className="mt-0.5 inline-flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-indigo-600 text-[10px] font-bold text-white">
                          {idx + 1}
                        </span>
                        <span>{rec}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>

            {/* Anomaly Alerts */}
            <div className="rounded-2xl border border-slate-200/80 bg-slate-50/60 p-4 dark:border-slate-800 dark:bg-slate-900/40">
              <h4 className="mb-2.5 flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-wider text-slate-700 dark:text-slate-200">
                <AlertTriangle size={14} className="text-amber-600 dark:text-amber-400" />
                <span>{isEn ? 'Operational anomalies' : 'Cảnh báo & bất thường'}</span>
              </h4>

              {summary.anomalies.length === 0 ? (
                <div className="flex items-center gap-2.5 rounded-xl border border-emerald-200 bg-emerald-50/80 p-3 text-xs font-semibold text-emerald-800 dark:border-emerald-500/30 dark:bg-emerald-950/35 dark:text-emerald-300">
                  <ShieldCheck size={16} className="shrink-0" />
                  <span>
                    {isEn
                      ? 'No operational bottlenecks or stock anomalies detected in this period.'
                      : 'Chưa ghi nhận điểm nghẽn bếp, quá hạn SLA hay rủi ro kho đáng chú ý trong kỳ này.'}
                  </span>
                </div>
              ) : (
                <div className="space-y-2.5">
                  {summary.anomalies.map((alert, idx) => {
                    const meta = getSeverityMeta(alert.severity, language);
                    return (
                      <div
                        key={idx}
                        className={`rounded-xl border p-3 ${meta.containerClass}`}
                      >
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-xs font-extrabold text-slate-900 dark:text-white">
                            {alert.title}
                          </span>
                          <span
                            className={`shrink-0 rounded-full border px-2 py-0.5 text-[10px] font-extrabold uppercase ${meta.badgeClass}`}
                          >
                            {meta.label}
                          </span>
                        </div>
                        <p className="mt-1 text-xs leading-relaxed text-slate-700 dark:text-slate-300">
                          {alert.detail}
                        </p>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </section>
  );
};

export default AiExecutiveSummaryCard;
