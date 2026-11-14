const StockBadge = ({ item }) => {
  if (item.active === false)
    return (
      <span className="shrink-0 rounded-full border border-slate-200/80 bg-slate-100 px-3 py-1 text-[10px] font-black uppercase tracking-wider text-slate-500 dark:border-slate-700/80 dark:bg-slate-800 dark:text-slate-400">
        Tạm ngưng
      </span>
    );
  if (item.outOfStock)
    return (
      <span className="shrink-0 rounded-full border border-rose-200/80 bg-rose-50 px-3 py-1 text-[10px] font-black uppercase tracking-wider text-rose-600 dark:border-rose-900/40 dark:bg-rose-950/40 dark:text-rose-300">
        Hết hàng
      </span>
    );
  if (item.lowStock)
    return (
      <span className="shrink-0 rounded-full border border-amber-200/80 bg-amber-50 px-3 py-1 text-[10px] font-black uppercase tracking-wider text-amber-700 dark:border-amber-800/40 dark:bg-amber-950/40 dark:text-amber-300">
        Sắp hết
      </span>
    );
  return (
    <span className="shrink-0 rounded-full border border-emerald-200/80 bg-emerald-50 px-3 py-1 text-[10px] font-black uppercase tracking-wider text-emerald-700 dark:border-emerald-800/40 dark:bg-emerald-950/40 dark:text-emerald-300">
      Ổn định
    </span>
  );
};

export default StockBadge;
