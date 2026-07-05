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
        className={`flex flex-1 items-center justify-center gap-2 rounded-xl border-2 py-3 font-bold transition-all ${
          paymentMethod === PAYMENT_METHODS.CASH
            ? 'border-green-500 bg-green-50 text-green-700 shadow-sm'
            : 'border-transparent bg-gray-100 text-gray-500 hover:bg-gray-200'
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
        className={`flex flex-1 items-center justify-center gap-2 rounded-xl border-2 py-3 font-bold transition-all ${
          paymentMethod === PAYMENT_METHODS.PAYOS
            ? 'border-blue-500 bg-blue-50 text-blue-700 shadow-sm'
            : 'border-transparent bg-gray-100 text-gray-500 hover:bg-gray-200'
        }`}
      >
        <QrCode size={20} /> Chuyển khoản
      </button>
    )}
  </div>
);

export default PaymentMethodTabs;
