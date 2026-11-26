import { fmtVND } from '@shared/lib/formatters.js';

const PaymentSummary = ({
  subtotalAmount,
  discountAmount,
  discountPromotion = 0,
  voucherDiscountAmount,
  serviceChargePercent = 0,
  serviceChargeAmount = 0,
  taxPercent = 0,
  taxAmount = 0,
  finalAmount,
}) => {
  const promoDiscount = Math.max(0, Number(discountPromotion || 0));
  const resolvedVoucherDiscount =
    voucherDiscountAmount != null
      ? Math.max(0, Number(voucherDiscountAmount))
      : Math.max(0, Number(discountAmount || 0) - promoDiscount);
  const serviceCharge = Math.max(0, Number(serviceChargeAmount || 0));
  const tax = Math.max(0, Number(taxAmount || 0));

  return (
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
        {promoDiscount > 0 && (
          <div className="flex justify-between text-sm">
            <span className="font-medium text-indigo-600 dark:text-indigo-400">
              Khuyến mãi khung giờ:
            </span>
            <span className="font-mono font-bold text-indigo-600 dark:text-indigo-400">
              -{fmtVND(promoDiscount)}
            </span>
          </div>
        )}
        {resolvedVoucherDiscount > 0 && (
          <div className="flex justify-between text-sm">
            <span className="font-medium text-emerald-600 dark:text-emerald-400">
              Giảm giá voucher:
            </span>
            <span className="font-mono font-bold text-emerald-600 dark:text-emerald-400">
              -{fmtVND(resolvedVoucherDiscount)}
            </span>
          </div>
        )}
        {serviceCharge > 0 && (
          <div className="flex justify-between text-sm">
            <span className="text-slate-500 dark:text-slate-400">
              Phí dịch vụ{Number(serviceChargePercent) > 0 ? ` (${serviceChargePercent}%)` : ''}:
            </span>
            <span className="font-mono font-bold text-slate-700 dark:text-slate-200">
              +{fmtVND(serviceCharge)}
            </span>
          </div>
        )}
        {tax > 0 && (
          <div className="flex justify-between text-sm">
            <span className="text-slate-500 dark:text-slate-400">
              Thuế VAT{Number(taxPercent) > 0 ? ` (${taxPercent}%)` : ''}:
            </span>
            <span className="font-mono font-bold text-slate-700 dark:text-slate-200">
              +{fmtVND(tax)}
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
};

export default PaymentSummary;
