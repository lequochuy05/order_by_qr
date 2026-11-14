import { fmtVND } from '@shared/lib/formatters.js';

const PaymentSummary = ({ subtotalAmount, discountAmount, finalAmount }) => (
  <div className="space-y-2.5">
    <label className="text-xs font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
      Chi tiết hóa đơn
    </label>
    <div className="space-y-3 rounded-2xl border border-slate-200/80 bg-slate-50/60 p-5 dark:border-slate-800/80 dark:bg-slate-950/60">
      <div className="flex justify-between text-sm">
        <span className="text-slate-500 dark:text-slate-400">Tạm tính:</span>
        <span className="font-mono font-bold text-slate-700 dark:text-slate-200">
          {fmtVND(subtotalAmount)}
        </span>
      </div>
      {discountAmount > 0 && (
        <div className="flex justify-between text-sm">
          <span className="font-medium text-emerald-600 dark:text-emerald-400">Giảm giá voucher:</span>
          <span className="font-mono font-bold text-emerald-600 dark:text-emerald-400">
            -{fmtVND(discountAmount)}
          </span>
        </div>
      )}
      <div className="mt-1 flex items-center justify-between border-t border-dashed border-slate-200/80 pt-3 dark:border-slate-800">
        <span className="font-bold text-slate-800 dark:text-slate-100">Cần thanh toán:</span>
        <span className="font-mono text-2xl font-black tracking-tight text-indigo-600 dark:text-indigo-400">
          {fmtVND(finalAmount)}
        </span>
      </div>
    </div>
  </div>
);

export default PaymentSummary;
