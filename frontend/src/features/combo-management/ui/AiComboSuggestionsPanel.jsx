import { useState } from 'react';
import {
  AlertTriangle,
  ChevronDown,
  ChevronUp,
  Loader2,
  PlusCircle,
  Sparkles,
} from 'lucide-react';

import { aiMerchandisingService } from '@entities/ai';
import { useAuth } from '@entities/session';
import {
  PLAN_FEATURES,
  ProBadge,
  UpgradeProModal,
  useSubscription,
} from '@entities/subscription';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { fmtVND } from '@shared/lib/formatters.js';
import { useTranslation } from '@shared/lib/i18n';

const AiComboSuggestionsPanel = ({ onUseDraft }) => {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const canViewBilling = isAdmin || user?.role === 'MANAGER';
  const { hasFeature } = useSubscription();
  const canUseAiOps = hasFeature(PLAN_FEATURES.AI_OPERATIONS_SUITE);
  const [upgradeOpen, setUpgradeOpen] = useState(false);
  const { language, isEn } = useTranslation('combos');
  const [expanded, setExpanded] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  const handleAnalyze = async () => {
    if (!canUseAiOps) {
      setUpgradeOpen(true);
      return;
    }
    if (loading) return;
    setExpanded(true);
    setLoading(true);
    setError('');
    try {
      const response = await aiMerchandisingService.suggestCombos({ language });
      setResult(response);
    } catch (err) {
      setError(buildErrorMessage(err, { includeDetails: false }));
    } finally {
      setLoading(false);
    }
  };

  const suggestions = result?.suggestions || [];

  return (
    <section
      aria-label={isEn ? 'AI Combo Bundle Recommendations' : 'AI Đề xuất tạo Combo'}
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
                {isEn ? 'AI Combo Bundle Builder' : 'AI Đề xuất tạo Combo'}
              </h2>
              {!canUseAiOps && <ProBadge size="xs" />}
              {result && (
                <span className="rounded-full border border-indigo-200 bg-indigo-50 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-indigo-700 dark:border-indigo-500/30 dark:bg-indigo-500/15 dark:text-indigo-300">
                  {result.aiGenerated ? 'Gemini AI' : isEn ? 'Order Co-occurrence' : 'Dữ liệu gọi kèm'}
                </span>
              )}
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {isEn
                ? 'Finds frequently paired dishes and pre-fills optimal bundle pricing.'
                : 'Phân tích món thường được gọi cùng nhau để đề xuất giá Combo hấp dẫn.'}
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
          className="inline-flex h-9 items-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-3.5 text-xs font-black text-white shadow-md shadow-indigo-600/20 transition hover:from-indigo-500 hover:to-violet-500 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
        >
          {loading ? <Loader2 size={14} className="animate-spin" /> : <Sparkles size={14} />}
          {loading
            ? isEn
              ? 'Generating...'
              : 'Đang phân tích...'
            : result
              ? isEn
                ? 'Refresh Ideas'
                : 'Gợi ý lại'
              : isEn
                ? 'Suggest Combos'
                : 'AI Gợi ý ghép Combo'}
        </button>
      </div>

      {expanded && (
        <div className="mt-4 space-y-4 border-t border-indigo-200/60 pt-4 dark:border-indigo-500/20">
          {loading && !result && (
            <div className="flex items-center justify-center gap-2 rounded-2xl border border-dashed border-indigo-200 bg-white/70 py-8 text-xs font-bold text-indigo-600 dark:border-indigo-500/25 dark:bg-slate-950/40 dark:text-indigo-300">
              <Loader2 size={16} className="animate-spin" />
              {isEn
                ? 'Mining co-occurrence orders & active menu margins...'
                : 'Đang phân tích dữ liệu đơn hàng gọi kèm...'}
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
              <p className="rounded-2xl border border-indigo-100 bg-white/90 px-4 py-2.5 text-xs font-bold text-slate-700 dark:border-indigo-500/20 dark:bg-slate-950/60 dark:text-slate-200">
                {result.summary}
              </p>

              {suggestions.length === 0 ? (
                <div className="rounded-2xl border border-dashed border-slate-200 bg-white/60 py-6 text-center text-xs font-semibold text-slate-500 dark:border-slate-800 dark:bg-slate-950/40 dark:text-slate-400">
                  {isEn
                    ? 'No new distinct combo bundles found beyond existing active combos.'
                    : 'Chưa tìm thấy cặp ghép Combo mới khác biệt với các Combo đang mở.'}
                </div>
              ) : (
                <div className="grid grid-cols-1 gap-3 lg:grid-cols-3">
                  {suggestions.map((proposal, index) => (
                    <article
                      key={`${proposal.name}-${index}`}
                      className="flex flex-col justify-between gap-3 rounded-2xl border border-slate-200/80 bg-white p-4 shadow-xs transition hover:border-indigo-300 dark:border-slate-800 dark:bg-slate-950/70 dark:hover:border-indigo-500/40"
                    >
                      <div className="space-y-2.5">
                        <div className="flex items-start justify-between gap-2">
                          <h3 className="text-sm font-black text-slate-900 dark:text-white">
                            {proposal.name}
                          </h3>
                          <span className="shrink-0 rounded-lg bg-emerald-100 px-2 py-0.5 font-mono text-[11px] font-black text-emerald-700 dark:bg-emerald-500/20 dark:text-emerald-300">
                            -{proposal.discountPercent}%
                          </span>
                        </div>

                        {proposal.description && (
                          <p className="text-xs text-slate-500 dark:text-slate-400">
                            {proposal.description}
                          </p>
                        )}

                        <div className="flex flex-wrap gap-1.5">
                          {(proposal.items || []).map((item) => (
                            <span
                              key={item.menuItemId}
                              className="rounded-lg border border-slate-200 bg-slate-50 px-2 py-0.5 text-[11px] font-semibold text-slate-700 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200"
                            >
                              {item.name} x{item.quantity}
                            </span>
                          ))}
                        </div>

                        <div className="flex items-baseline gap-2 pt-1">
                          <span className="font-mono text-base font-black text-indigo-600 dark:text-indigo-400">
                            {fmtVND(proposal.suggestedPrice)}
                          </span>
                          <span className="font-mono text-xs text-slate-400 line-through">
                            {fmtVND(proposal.originalTotalPrice)}
                          </span>
                        </div>

                        {proposal.rationale && (
                          <p className="text-[11px] italic text-slate-600 dark:text-slate-300">
                            {proposal.rationale}
                          </p>
                        )}
                      </div>

                      <button
                        type="button"
                        onClick={() => onUseDraft?.(proposal, index)}
                        className="inline-flex h-9 w-full items-center justify-center gap-1.5 rounded-xl border border-indigo-200 bg-indigo-50 text-xs font-black text-indigo-700 transition hover:bg-indigo-600 hover:text-white active:scale-[0.98] dark:border-indigo-500/30 dark:bg-indigo-500/15 dark:text-indigo-300 dark:hover:bg-indigo-600 dark:hover:text-white"
                      >
                        <PlusCircle size={14} />
                        {isEn ? 'Create Combo from Draft' : 'Tạo Combo từ gợi ý'}
                      </button>
                    </article>
                  ))}
                </div>
              )}
            </>
          )}
        </div>
      )}

      <UpgradeProModal
        isOpen={upgradeOpen}
        feature={PLAN_FEATURES.AI_OPERATIONS_SUITE}
        isAdmin={isAdmin}
        canViewBilling={canViewBilling}
        onClose={() => setUpgradeOpen(false)}
      />
    </section>
  );
};

export default AiComboSuggestionsPanel;
