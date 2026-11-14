import { Banknote, QrCode } from 'lucide-react';

import { PAYMENT_METHODS } from '@entities/payment/lib/paymentStatus.js';

const PaymentMethodTabs = ({
  paymentMethod,
  payosStatus,
  cashEnabled,
  onlineEnabled,
  onSelectCash,
  onSelectPayos,
}) => (
  <div className="flex gap-3">
    {cashEnabled && (
      <button
        type="button"
        onClick={onSelectCash}
        disabled={payosStatus === 'waiting'}
        className={`flex flex-1 items-center justify-center gap-2 rounded-2xl border-2 py-3 text-sm font-bold transition-all active:scale-[0.98] ${
          paymentMethod === PAYMENT_METHODS.CASH
            ? 'border-emerald-500 bg-emerald-50 text-emerald-700 shadow-sm dark:border-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-300'
            : 'border-transparent bg-slate-100 text-slate-500 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700'
        } ${payosStatus === 'waiting' ? 'cursor-not-allowed opacity-50' : ''}`}
      >
        <Banknote size={20} /> Tiền mặt
      </button>
    )}
    {onlineEnabled && (
      <button
        type="button"
        onClick={onSelectPayos}
        disabled={payosStatus === 'waiting' && paymentMethod !== PAYMENT_METHODS.PAYOS}
        className={`flex flex-1 items-center justify-center gap-2 rounded-2xl border-2 py-3 text-sm font-bold transition-all active:scale-[0.98] ${
          paymentMethod === PAYMENT_METHODS.PAYOS
            ? 'border-indigo-500 bg-indigo-50 text-indigo-700 shadow-sm dark:border-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-300'
            : 'border-transparent bg-slate-100 text-slate-500 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700'
        }`}
      >
        <QrCode size={20} /> Chuyển khoản
      </button>
    )}
  </div>
);

export default PaymentMethodTabs;
