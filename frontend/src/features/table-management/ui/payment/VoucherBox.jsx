import { CheckCircle2, Tag } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';

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
    <label className="text-xs font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
      Ưu đãi & Khuyến mãi
    </label>
    <div className="flex gap-2">
      <div className="relative flex-1">
        <Tag className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500" size={17} />
        <input
          type="text"
          className="w-full rounded-2xl border border-slate-200/80 bg-white py-2.5 pl-10 pr-4 text-sm font-semibold uppercase text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700/80 dark:bg-slate-950 dark:text-slate-100 dark:placeholder:text-slate-600"
          placeholder="NHẬP MÃ GIẢM GIÁ"
          value={voucherCode}
          onChange={onInputChange}
        />
      </div>
      <button
        type="button"
        onClick={onApply}
        disabled={loading || !voucherCode.trim()}
        className="rounded-2xl bg-gradient-to-r from-indigo-600 to-violet-600 px-5 text-xs font-bold text-white shadow-md shadow-indigo-600/25 transition-all hover:from-indigo-500 hover:to-violet-500 active:scale-95 disabled:opacity-50 disabled:shadow-none"
      >
        Áp dụng
      </button>
    </div>
    {error && <p className="ml-1 text-xs font-semibold text-rose-500">{error}</p>}
    {voucherRequiresApply && !error && (
      <p className="ml-1 text-xs font-semibold text-amber-600 dark:text-amber-400">
        Mã voucher chưa được áp dụng.
      </p>
    )}
    {voucherValid && voucherCode && !voucherRequiresApply && (
      <p className="ml-1 flex items-center gap-1 font-mono text-xs font-bold text-emerald-600 dark:text-emerald-400">
        <CheckCircle2 size={13} /> Voucher hợp lệ: -{fmtVND(discountAmount)}
      </p>
    )}
  </div>
);

export default VoucherBox;
