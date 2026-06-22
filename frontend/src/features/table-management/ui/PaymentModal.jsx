import { X } from 'lucide-react';

import usePaymentModal from '@features/table-management/model/usePaymentModal.js';
import { ErrorBoundary } from '@shared/ui';
import SharedModal from '@shared/ui/SharedModal.jsx';
import PaymentActions from './payment/PaymentActions.jsx';
import PaymentMethodTabs from './payment/PaymentMethodTabs.jsx';
import PaymentSummary from './payment/PaymentSummary.jsx';
import PayosQrPanel from './payment/PayosQrPanel.jsx';
import VoucherBox from './payment/VoucherBox.jsx';

const PaymentModalContent = (props) => {
  const { isOpen, onClose, table, order } = props;
  const payment = usePaymentModal(props);

  if (!isOpen || !table) return null;

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      className="max-w-lg !overflow-hidden !p-0"
      ariaLabel={`Thanh toán hóa đơn bàn ${table.tableNumber}`}
    >
      <div className="flex shrink-0 items-center justify-between border-b bg-gray-50 px-6 py-4">
        <div>
          <h3 className="text-lg font-bold">Thanh toán hóa đơn</h3>
          <p className="text-xs font-semibold uppercase tracking-wider text-gray-500">
            Bàn {table.tableNumber} • #{order.id.toString().slice(-6)}
          </p>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="rounded-full p-2 transition-colors hover:bg-gray-200"
          aria-label="Đóng"
        >
          <X size={20} className="text-gray-500" />
        </button>
      </div>

      <div className="flex-1 space-y-6 overflow-y-auto p-6">
        <PaymentMethodTabs
          paymentMethod={payment.paymentMethod}
          payosStatus={payment.payosStatus}
          onSelectCash={payment.selectCash}
          onSelectPayos={() => payment.setPaymentMethod('PAYOS')}
        />

        {payment.paymentMethod === 'PAYOS' ? (
          <PayosQrPanel
            status={payment.payosStatus}
            data={payment.payosData}
            loading={payment.payosLoading}
            timeLeft={payment.timeLeft}
            error={payment.error}
            onCreate={payment.handleCreatePayosQR}
            onCancel={payment.handleCancelPayos}
          />
        ) : (
          <VoucherBox
            voucherCode={payment.voucherCode}
            onInputChange={payment.handleInputChange}
            onApply={payment.handleApplyVoucher}
            loading={payment.loading}
            error={payment.error}
            voucherValid={payment.previewData?.voucherValid}
            discountAmount={payment.amounts.discountAmount}
          />
        )}

        <PaymentSummary {...payment.amounts} />
      </div>

      <PaymentActions
        paymentMethod={payment.paymentMethod}
        loading={payment.loading}
        onCashPay={payment.handleConfirmCashPay}
        onClose={onClose}
      />
    </SharedModal>
  );
};

const PaymentModal = (props) => (
  <ErrorBoundary>
    <PaymentModalContent {...props} />
  </ErrorBoundary>
);

export default PaymentModal;
