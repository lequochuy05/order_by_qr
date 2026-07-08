const StockBadge = ({ item }) => {
	  if (item.active === false)
	    return (
	      <span className="shrink-0 rounded-full bg-slate-100 px-3 py-1 text-[10px] font-black uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
	        Tạm ngưng
	      </span>
    );
  if (item.outOfStock)
    return (
      <span className="shrink-0 rounded-full bg-red-100 px-3 py-1 text-[10px] font-black uppercase text-red-600 dark:bg-red-500/10 dark:text-red-300">
        Hết hàng
      </span>
    );
  if (item.lowStock)
    return (
      <span className="shrink-0 rounded-full bg-amber-100 px-3 py-1 text-[10px] font-black uppercase text-amber-700 dark:bg-amber-500/10 dark:text-amber-300">
        Sắp hết
      </span>
    );
  return (
    <span className="shrink-0 rounded-full bg-emerald-100 px-3 py-1 text-[10px] font-black uppercase text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-300">
      Ổn định
    </span>
  );
};

export default StockBadge;
