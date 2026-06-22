import { CheckCircle2, Tag } from 'lucide-react';

const VoucherBox = ({
  voucherCode,
  onInputChange,
  onApply,
  loading,
  error,
  voucherValid,
  discountAmount,
}) => (
  <div className="space-y-3 animate-in slide-in-from-bottom-2 duration-300">
    <label className="text-xs font-bold uppercase text-gray-400">Ưu đãi & Khuyến mãi</label>
    <div className="flex gap-2">
      <div className="relative flex-1">
        <Tag className="absolute left-3 top-3 text-gray-400" size={18} />
        <input
          type="text"
          className="w-full rounded-xl border py-2.5 pl-10 pr-4 text-sm font-semibold uppercase outline-none focus:ring-2 focus:ring-orange-500"
          placeholder="NHẬP MÃ GIẢM GIÁ"
          value={voucherCode}
          onChange={onInputChange}
        />
      </div>
      <button
        type="button"
        onClick={onApply}
        disabled={loading || !voucherCode}
        className="rounded-xl bg-gray-800 px-4 font-bold text-white transition-colors hover:bg-black disabled:opacity-50"
      >
        Áp dụng
      </button>
    </div>
    {error && <p className="ml-1 text-xs font-medium text-red-500">{error}</p>}
    {voucherValid && voucherCode && (
      <p className="ml-1 flex items-center gap-1 text-xs font-bold text-green-600">
        <CheckCircle2 size={12} /> Voucher hợp lệ: -{discountAmount.toLocaleString()}đ
      </p>
    )}
  </div>
);

export default VoucherBox;
