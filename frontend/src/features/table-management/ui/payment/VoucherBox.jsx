import { CheckCircle2, Tag } from 'lucide-react';

const VoucherBox = ({
  voucherCode,
  onInputChange,
  onApply,
  loading,
  error,
  voucherValid,
  voucherRequiresApply,
  discountAmount,
}) => (
  <div className="space-y-3 animate-in slide-in-from-bottom-2 duration-300">
    <label className="text-xs font-bold uppercase text-slate-400 dark:text-slate-500">
      Ưu đãi & Khuyến mãi
    </label>
    <div className="flex gap-2">
      <div className="relative flex-1">
        <Tag className="absolute left-3 top-3 text-slate-400 dark:text-slate-500" size={18} />
        <input
          type="text"
          className="w-full rounded-xl border border-slate-200 bg-white py-2.5 pl-10 pr-4 text-sm font-semibold uppercase text-slate-900 outline-none transition placeholder:text-slate-400 focus:ring-2 focus:ring-orange-500 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100 dark:placeholder:text-slate-600"
          placeholder="NHẬP MÃ GIẢM GIÁ"
          value={voucherCode}
          onChange={onInputChange}
        />
      </div>
      <button
        type="button"
        onClick={onApply}
        disabled={loading || !voucherCode.trim()}
        className="rounded-xl bg-slate-800 px-4 font-bold text-white transition-colors hover:bg-black disabled:opacity-50 dark:bg-slate-100 dark:text-slate-950 dark:hover:bg-white"
      >
        Áp dụng
      </button>
    </div>
    {error && <p className="ml-1 text-xs font-medium text-red-500">{error}</p>}
    {voucherRequiresApply && !error && (
      <p className="ml-1 text-xs font-semibold text-amber-600">
        Mã voucher chưa được áp dụng.
      </p>
    )}
    {voucherValid && voucherCode && !voucherRequiresApply && (
      <p className="ml-1 flex items-center gap-1 text-xs font-bold text-green-600 dark:text-green-400">
        <CheckCircle2 size={12} /> Voucher hợp lệ: -{discountAmount.toLocaleString()}đ
      </p>
    )}
  </div>
);

export default VoucherBox;
