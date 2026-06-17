const COLOR_CLASSES = {
  amber: { bg: 'bg-amber-50 dark:bg-amber-500/10', text: 'text-amber-700 dark:text-amber-300', border: 'border-amber-200 dark:border-amber-500/30' },
  blue: { bg: 'bg-blue-50 dark:bg-blue-500/10', text: 'text-blue-700 dark:text-blue-300', border: 'border-blue-200 dark:border-blue-500/30' },
  cyan: { bg: 'bg-cyan-50 dark:bg-cyan-500/10', text: 'text-cyan-700 dark:text-cyan-300', border: 'border-cyan-200 dark:border-cyan-500/30' },
  emerald: { bg: 'bg-emerald-50 dark:bg-emerald-500/10', text: 'text-emerald-700 dark:text-emerald-300', border: 'border-emerald-200 dark:border-emerald-500/30' },
  rose: { bg: 'bg-rose-50 dark:bg-rose-500/10', text: 'text-rose-700 dark:text-rose-300', border: 'border-rose-200 dark:border-rose-500/30' },
  indigo: { bg: 'bg-indigo-50 dark:bg-indigo-500/10', text: 'text-indigo-700 dark:text-indigo-300', border: 'border-indigo-200 dark:border-indigo-500/30' },
  slate: { bg: 'bg-slate-50 dark:bg-slate-800', text: 'text-slate-600 dark:text-slate-300', border: 'border-slate-200 dark:border-slate-700' },
  orange: { bg: 'bg-orange-50 dark:bg-orange-500/10', text: 'text-orange-600 dark:text-orange-300', border: 'border-orange-400 dark:border-orange-500/30' },
};

const FALLBACK_COLOR = COLOR_CLASSES.slate;

export const getStatusClasses = (colorName) => {
  const color = COLOR_CLASSES[colorName] || FALLBACK_COLOR;
  return `${color.bg} ${color.text} ${color.border}`;
};
