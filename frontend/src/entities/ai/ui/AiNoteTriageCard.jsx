import { useState } from 'react';
import {
  AlertTriangle,
  BellRing,
  ChevronDown,
  ChevronUp,
  Clock3,
  Crown,
  Loader2,
  ShieldAlert,
  Sparkles,
  UtensilsCrossed,
} from 'lucide-react';

import { aiStaffService } from '../api/aiService.js';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { useTranslation } from '@shared/lib/i18n';
import { RISK_LEVELS } from '../lib/noteSafety.js';
import NoteSafetyBadges from './NoteSafetyBadges.jsx';

const AiNoteTriageCard = ({ namespace = 'kitchen', locked = false, onRequestUpgrade = null }) => {
  const { language, isEn } = useTranslation(namespace);
  const [expanded, setExpanded] = useState(false);
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
      const response = await aiStaffService.analyzeNotes({ language });
      setResult(response);
    } catch (err) {
      setError(buildErrorMessage(err, { includeDetails: false }));
    } finally {
      setLoading(false);
    }
  };

  const alerts = result?.alerts || [];

  return (
    <section
      aria-label={
        isEn
          ? 'AI Allergy & Priority Note Triage'
          : 'AI Cảnh báo dị ứng & Ưu tiên ghi chú khách'
      }
      className="rounded-3xl border border-rose-200/80 bg-gradient-to-br from-rose-50/60 via-white to-amber-50/50 p-4 shadow-sm transition-colors sm:p-5 dark:border-rose-500/25 dark:from-rose-950/25 dark:via-slate-900 dark:to-amber-950/15"
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <button
          type="button"
          onClick={() => setExpanded((prev) => !prev)}
          className="flex min-w-0 items-center gap-3 text-left"
        >
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-rose-600 to-amber-500 text-white shadow-md shadow-rose-500/20">
            <ShieldAlert size={18} />
          </div>
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-sm font-black text-slate-900 sm:text-base dark:text-white">
                {isEn
                  ? 'AI Allergy & Note Triage'
                  : 'AI Cảnh báo dị ứng & Ưu tiên ghi chú'}
              </h2>
              {locked && (
                <span className="inline-flex items-center gap-1 rounded-full border border-amber-300/80 bg-amber-50 px-1.5 py-0.5 text-[10px] font-black uppercase tracking-wider text-amber-700 dark:border-amber-500/40 dark:bg-amber-500/15 dark:text-amber-300">
                  <Crown size={10} />
                  <span>PRO</span>
                </span>
              )}
              {result && (
                <span className="rounded-full border border-rose-200 bg-rose-50 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/15 dark:text-rose-300">
                  {result.aiGenerated ? 'Gemini AI' : isEn ? 'Safety Rules' : 'Bộ lọc an toàn'}
                </span>
              )}
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {isEn
                ? 'Scans open kitchen & table notes to surface allergy risks and urgent needs first.'
                : 'Quét toàn bộ ghi chú món bếp & yêu cầu bàn để xếp ưu tiên dị ứng và xử lý gấp.'}
            </p>
          </div>
          <span className="ml-1 text-slate-400 dark:text-slate-500">
            {expanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
          </span>
        </button>

        <button
          type="button"
          onClick={handleAnalyze}
          disabled={loading}
          className="inline-flex h-9 items-center gap-2 rounded-xl bg-gradient-to-r from-rose-600 to-amber-600 px-3.5 text-xs font-black text-white shadow-md shadow-rose-600/20 transition hover:from-rose-500 hover:to-amber-500 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
        >
          {loading ? <Loader2 size={14} className="animate-spin" /> : <Sparkles size={14} />}
          {loading
            ? isEn
              ? 'Scanning notes...'
              : 'Đang phân tích...'
            : result
              ? isEn
                ? 'Re-scan Notes'
                : 'Quét lại ghi chú'
              : isEn
                ? 'Scan Open Notes'
                : 'AI Phân loại ghi chú'}
        </button>
      </div>

      {expanded && (
        <div className="mt-4 space-y-3 border-t border-rose-200/60 pt-4 dark:border-rose-500/20">
          {loading && !result && (
            <div className="flex items-center justify-center gap-2 rounded-2xl border border-dashed border-rose-200 bg-white/70 py-6 text-xs font-bold text-rose-600 dark:border-rose-500/25 dark:bg-slate-950/40 dark:text-rose-300">
              <Loader2 size={16} className="animate-spin" />
              {isEn
                ? 'Auditing active kitchen & service notes for allergens...'
                : 'Đang kiểm tra dị ứng và mức ưu tiên trên các ghi chú đang mở...'}
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
              <div className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-rose-100 bg-white/90 px-4 py-2.5 dark:border-rose-500/20 dark:bg-slate-950/60">
                <p className="text-xs font-bold text-slate-800 dark:text-slate-100">
                  {result.summary}
                </p>
                <div className="flex items-center gap-2 text-[11px] font-black">
                  <span className="rounded-lg bg-rose-100 px-2 py-0.5 text-rose-700 dark:bg-rose-500/20 dark:text-rose-200">
                    {result.criticalCount} {isEn ? 'critical' : 'dị ứng / khẩn cấp'}
                  </span>
                  <span className="rounded-lg bg-amber-100 px-2 py-0.5 text-amber-800 dark:bg-amber-500/20 dark:text-amber-200">
                    {result.highCount} {isEn ? 'priority' : 'ưu tiên cao'}
                  </span>
                </div>
              </div>

              {alerts.length === 0 ? (
                <div className="rounded-2xl border border-dashed border-slate-200 bg-white/60 py-5 text-center text-xs font-semibold text-slate-500 dark:border-slate-800 dark:bg-slate-950/40 dark:text-slate-400">
                  {isEn
                    ? 'No active kitchen or table notes require safety triage.'
                    : 'Hiện không có ghi chú đang mở nào cần phân loại.'}
                </div>
              ) : (
                <div className="max-h-80 space-y-2.5 overflow-y-auto pr-1">
                  {alerts.map((alert) => {
                    const isCritical = alert.riskLevel === RISK_LEVELS.CRITICAL;
                    const isHigh = alert.riskLevel === RISK_LEVELS.HIGH;
                    const rowSurface = isCritical
                      ? 'border-rose-300 bg-rose-50/70 dark:border-rose-500/40 dark:bg-rose-950/25'
                      : isHigh
                        ? 'border-amber-300 bg-amber-50/60 dark:border-amber-500/40 dark:bg-amber-950/20'
                        : 'border-slate-200/80 bg-white dark:border-slate-800 dark:bg-slate-950/70';

                    return (
                      <article
                        key={`${alert.sourceType}-${alert.sourceId}`}
                        className={`rounded-2xl border p-3.5 transition ${rowSurface}`}
                      >
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="rounded-lg bg-slate-900 px-2 py-0.5 text-xs font-black text-white dark:bg-slate-700">
                              {alert.tableLabel}
                            </span>
                            <span className="inline-flex items-center gap-1 rounded-md border border-slate-200 bg-white px-2 py-0.5 text-[10px] font-bold text-slate-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300">
                              {alert.sourceType === 'KITCHEN_ITEM' ? (
                                <UtensilsCrossed size={11} />
                              ) : (
                                <BellRing size={11} />
                              )}
                              {alert.sourceType === 'KITCHEN_ITEM'
                                ? isEn
                                  ? 'Kitchen'
                                  : 'Món bếp'
                                : isEn
                                  ? 'Table Request'
                                  : 'Yêu cầu bàn'}
                            </span>
                            <h3 className="text-xs font-black text-slate-900 sm:text-sm dark:text-white">
                              {alert.itemName}
                            </h3>
                          </div>

                          <span className="inline-flex items-center gap-1 font-mono text-[11px] font-bold text-slate-500 dark:text-slate-400">
                            <Clock3 size={12} />
                            {alert.waitMinutes} {isEn ? 'mins' : 'phút'}
                          </span>
                        </div>

                        <div className="mt-2">
                          <NoteSafetyBadges
                            note={alert.rawNote}
                            extraAllergens={alert.allergens}
                            riskOverride={alert.riskLevel}
                            language={language}
                            compact
                          />
                        </div>

                        <p className="mt-2 text-xs italic text-slate-700 dark:text-slate-200">
                          “{alert.rawNote}”
                        </p>

                        {alert.actionInstruction && (
                          <p className="mt-1.5 rounded-xl bg-white/80 px-2.5 py-1.5 text-xs font-semibold text-slate-800 dark:bg-slate-900/70 dark:text-slate-200">
                            → {alert.actionInstruction}
                          </p>
                        )}
                      </article>
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

export default AiNoteTriageCard;
