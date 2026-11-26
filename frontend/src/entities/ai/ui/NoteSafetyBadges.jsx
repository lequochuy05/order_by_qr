import { AlertTriangle, Leaf, ShieldAlert, Zap } from 'lucide-react';
import {
  analyzeCustomerNote,
  getSafetyFlagLabel,
  maxRisk,
  RISK_LEVELS,
} from '../lib/noteSafety.js';

const NoteSafetyBadges = ({
  note,
  safety = null,
  extraAllergens = [],
  riskOverride = null,
  language = 'vi',
  compact = false,
}) => {
  const base = safety || analyzeCustomerNote(note);
  const allergens = Array.from(new Set([...(base.allergens || []), ...(extraAllergens || [])]));
  const urgencyFlags = base.urgencyFlags || [];
  const dietaryFlags = base.dietaryFlags || [];

  let riskLevel = maxRisk(base.riskLevel, riskOverride);
  if (allergens.length > 0) {
    riskLevel = RISK_LEVELS.CRITICAL;
  }

  if (
    allergens.length === 0 &&
    urgencyFlags.length === 0 &&
    dietaryFlags.length === 0 &&
    riskLevel === RISK_LEVELS.NORMAL
  ) {
    return null;
  }

  const sizeClass = compact ? 'px-1.5 py-0.5 text-[10px]' : 'px-2 py-0.5 text-[11px]';

  return (
    <div className="flex flex-wrap items-center gap-1.5">
      {riskLevel === RISK_LEVELS.CRITICAL && (
        <span
          className={`inline-flex items-center gap-1 rounded-md border border-rose-300 bg-rose-100 font-black uppercase tracking-wide text-rose-800 dark:border-rose-500/40 dark:bg-rose-500/20 dark:text-rose-200 ${sizeClass}`}
        >
          <ShieldAlert size={compact ? 11 : 12} className="shrink-0" />
          {language === 'en' ? 'Allergy Alert' : 'Cảnh báo dị ứng'}
        </span>
      )}

      {allergens.map((code) => (
        <span
          key={`alg-${code}`}
          className={`inline-flex items-center gap-1 rounded-md border border-rose-200 bg-rose-50 font-bold text-rose-700 dark:border-rose-500/30 dark:bg-rose-950/60 dark:text-rose-300 ${sizeClass}`}
        >
          <AlertTriangle size={compact ? 10 : 11} className="shrink-0" />
          {getSafetyFlagLabel(code, language)}
        </span>
      ))}

      {urgencyFlags.map((code) => (
        <span
          key={`urg-${code}`}
          className={`inline-flex items-center gap-1 rounded-md border border-amber-300 bg-amber-100 font-extrabold text-amber-900 dark:border-amber-500/40 dark:bg-amber-500/20 dark:text-amber-200 ${sizeClass}`}
        >
          <Zap size={compact ? 10 : 11} className="shrink-0" />
          {getSafetyFlagLabel(code, language)}
        </span>
      ))}

      {dietaryFlags.map((code) => (
        <span
          key={`diet-${code}`}
          className={`inline-flex items-center gap-1 rounded-md border border-indigo-200 bg-indigo-50 font-semibold text-indigo-700 dark:border-indigo-500/30 dark:bg-indigo-950/50 dark:text-indigo-300 ${sizeClass}`}
        >
          <Leaf size={compact ? 10 : 11} className="shrink-0" />
          {getSafetyFlagLabel(code, language)}
        </span>
      ))}
    </div>
  );
};

export default NoteSafetyBadges;
