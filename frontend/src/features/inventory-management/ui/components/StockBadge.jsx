const StockBadge = ({ item }) => {
  if (item.active === false)
    return (
      <span className="shrink-0 rounded-full bg-gray-100 px-3 py-1 text-[10px] font-black uppercase text-gray-500">
        Tạm ngưng
      </span>
    );
  if (item.outOfStock)
    return (
      <span className="shrink-0 rounded-full bg-red-100 px-3 py-1 text-[10px] font-black uppercase text-red-600">
        Hết hàng
      </span>
    );
  if (item.lowStock)
    return (
      <span className="shrink-0 rounded-full bg-amber-100 px-3 py-1 text-[10px] font-black uppercase text-amber-700">
        Sắp hết
      </span>
    );
  return (
    <span className="shrink-0 rounded-full bg-emerald-100 px-3 py-1 text-[10px] font-black uppercase text-emerald-700">
      Ổn định
    </span>
  );
};

export default StockBadge;
