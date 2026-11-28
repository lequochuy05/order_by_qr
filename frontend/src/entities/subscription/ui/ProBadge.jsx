import { Crown } from 'lucide-react';

const ProBadge = ({ label = 'PRO', size = 'sm', className = '' }) => {
  const sizeClass =
    size === 'xs'
      ? 'px-1.5 py-0.5 text-[10px] gap-1'
      : 'px-2 py-0.5 text-[11px] gap-1';

  return (
    <span
      className={`inline-flex items-center rounded-full border border-amber-300/80 bg-amber-50 font-black uppercase tracking-wider text-amber-700 dark:border-amber-500/40 dark:bg-amber-500/15 dark:text-amber-300 ${sizeClass} ${className}`}
    >
      <Crown size={size === 'xs' ? 10 : 12} className="shrink-0" />
      <span>{label}</span>
    </span>
  );
};

export default ProBadge;
