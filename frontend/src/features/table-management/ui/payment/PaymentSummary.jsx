const PaymentSummary = ({ subtotalAmount, discountAmount, finalAmount }) => (
  <div className="space-y-3">
    <label className="text-xs font-bold uppercase text-slate-400 dark:text-slate-500">
      Chi tiết hóa đơn
    </label>
    <div className="space-y-3 rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-800 dark:bg-slate-950/60">
      <div className="flex justify-between text-sm">
        <span className="text-slate-500 dark:text-slate-400">Tạm tính:</span>
        <span className="font-semibold text-slate-700 dark:text-slate-200">
          {subtotalAmount.toLocaleString()}đ
        </span>
      </div>
      {discountAmount > 0 && (
        <div className="flex justify-between text-sm">
          <span className="font-medium text-green-600 dark:text-green-400">Giảm giá voucher:</span>
          <span className="font-bold text-green-600 dark:text-green-400">-{discountAmount.toLocaleString()}đ</span>
        </div>
      )}
      <div className="mt-1 flex items-center justify-between border-t border-dashed border-slate-300 pt-3 dark:border-slate-700">
        <span className="font-bold text-slate-800 dark:text-slate-100">Cần thanh toán:</span>
        <span className="text-2xl font-black text-orange-600 dark:text-orange-300">
          {finalAmount.toLocaleString()}đ
        </span>
      </div>
    </div>
  </div>
);

export default PaymentSummary;
