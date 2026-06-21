import { CreditCard, Loader2 } from 'lucide-react';

const PaymentActions = ({ paymentMethod, loading, onCashPay, onClose }) => (
  <div className="shrink-0 border-t bg-white px-6 py-5">
    {paymentMethod === 'CASH' ? (
      <button
        type="button"
        onClick={onCashPay}
        disabled={loading}
        className="flex w-full items-center justify-center gap-3 rounded-2xl bg-green-600 py-4 text-lg font-black text-white shadow-xl shadow-green-100 transition-all hover:bg-green-700 active:scale-[0.98]"
      >
        {loading ? <Loader2 className="animate-spin" /> : <CreditCard size={24} />}
        XÁC NHẬN THANH TOÁN
      </button>
    ) : (
      <button
        type="button"
        onClick={onClose}
        className="w-full rounded-xl py-3 font-bold text-gray-500 transition-colors hover:bg-gray-100"
      >
        Quay lại
      </button>
    )}
  </div>
);

export default PaymentActions;
