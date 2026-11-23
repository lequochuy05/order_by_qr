const TONE_STYLES = {
  indigo: {
    cardActive:
      'border-indigo-500/35 bg-indigo-50/55 dark:border-indigo-500/35 dark:bg-indigo-950/30',
    trackActive: 'bg-gradient-to-r from-indigo-600 to-violet-600 shadow-sm shadow-indigo-500/30',
    focusRing: 'peer-focus-visible:ring-indigo-500/25',
  },
  orange: {
    cardActive:
      'border-orange-500/35 bg-orange-50/55 dark:border-orange-500/35 dark:bg-orange-950/25',
    trackActive: 'bg-orange-500 shadow-sm shadow-orange-500/30',
    focusRing: 'peer-focus-visible:ring-orange-500/25',
  },
  green: {
    cardActive:
      'border-emerald-500/35 bg-emerald-50/55 dark:border-emerald-500/35 dark:bg-emerald-950/30',
    trackActive: 'bg-emerald-500 shadow-sm shadow-emerald-500/30',
    focusRing: 'peer-focus-visible:ring-emerald-500/25',
  },
  red: {
    cardActive: 'border-rose-500/35 bg-rose-50/55 dark:border-rose-500/35 dark:bg-rose-950/30',
    trackActive: 'bg-rose-500 shadow-sm shadow-rose-500/30',
    focusRing: 'peer-focus-visible:ring-rose-500/25',
  },
};

const CheckboxCard = ({
  checked,
  onChange,
  label,
  description,
  tone = 'indigo',
  disabled = false,
  className = '',
}) => {
  const isChecked = Boolean(checked);
  const styles = TONE_STYLES[tone] || TONE_STYLES.indigo;

  return (
    <label
      className={`flex min-h-[52px] w-full cursor-pointer items-center justify-between gap-3 rounded-2xl border-2 px-4 py-2.5 transition-all select-none ${
        isChecked
          ? styles.cardActive
          : 'border-slate-200/75 bg-slate-50/80 hover:border-slate-300/80 hover:bg-slate-100/70 dark:border-slate-800/90 dark:bg-slate-950 dark:hover:border-slate-700 dark:hover:bg-slate-900'
      } ${disabled ? 'cursor-not-allowed opacity-60' : ''} ${className}`}
    >
      <input
        type="checkbox"
        className="peer sr-only"
        checked={isChecked}
        onChange={(event) => onChange?.(event.target.checked, event)}
        disabled={disabled}
      />
      <span className="min-w-0 flex-1">
        <span className="block text-xs font-extrabold tracking-tight text-slate-700 dark:text-slate-100">
          {label}
        </span>
        {description && (
          <span className="mt-0.5 block text-[11px] font-medium leading-snug text-slate-500 dark:text-slate-400">
            {description}
          </span>
        )}
      </span>
      <span
        aria-hidden="true"
        className={`relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-all duration-200 peer-focus-visible:ring-4 ${styles.focusRing} ${
          isChecked ? styles.trackActive : 'bg-slate-300/80 dark:bg-slate-700'
        }`}
      >
        <span
          className={`inline-block h-4.5 w-4.5 transform rounded-full bg-white shadow-sm transition-transform duration-200 ${
            isChecked ? 'translate-x-[22px]' : 'translate-x-[3px]'
          }`}
        />
      </span>
    </label>
  );
};

export default CheckboxCard;
