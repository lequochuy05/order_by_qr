import { X } from 'lucide-react';

import { PAYMENT_METHODS } from '@entities/payment/lib/paymentStatus.js';
import usePaymentModal from '../model/usePaymentModal.js';
import { ErrorBoundary } from '@shared/ui';
import SharedModal from '@shared/ui/SharedModal.jsx';
import PaymentActions from './payment/PaymentActions.jsx';
import PaymentMethodTabs from './payment/PaymentMethodTabs.jsx';
import PaymentSummary from './payment/PaymentSummary.jsx';
import PayosQrPanel from './payment/PayosQrPanel.jsx';
import VoucherBox from './payment/VoucherBox.jsx';
import CashTenderedPanel from './payment/CashTenderedPanel.jsx';

const PaymentModalContent = (props) => {
  const { isOpen, onClose, table, order } = props;
  const payment = usePaymentModal(props);

  if (!isOpen || !table || !order) return null;

  const orderTitle = table?.displayName || (table?.tableNumber ? `Bàn ${table.tableNumber}` : 'Đơn mang về');
  const orderCode = order?.id ? `#${order.id.toString().slice(-6)}` : '';

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      className="max-w-lg !overflow-hidden !p-0"
      ariaLabel={`Thanh toán ${orderTitle}`}
    >
      <div className="flex shrink-0 items-center justify-between border-b border-slate-100 bg-slate-50 px-6 py-4 transition-colors dark:border-slate-800 dark:bg-slate-950/60">
        <div>
          <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">Thanh toán hóa đơn</h3>
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
            {orderTitle} {orderCode ? `• ${orderCode}` : ''}
          </p>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="rounded-full p-2 transition-colors hover:bg-slate-200 dark:hover:bg-slate-800"
          aria-label="Đóng"
        >
          <X size={20} className="text-slate-500 dark:text-slate-300" />
        </button>
      </div>

      <div className="flex-1 space-y-6 overflow-y-auto p-6">
        <PaymentMethodTabs
          paymentMethod={payment.paymentMethod}
          payosStatus={payment.payosStatus}
          cashEnabled={payment.cashPaymentEnabled}
          onlineEnabled={payment.onlinePaymentEnabled}
          onSelectCash={payment.selectCash}
          onSelectPayos={() => payment.setPaymentMethod(PAYMENT_METHODS.PAYOS)}
        />

        {payment.paymentMethod === PAYMENT_METHODS.PAYOS ? (
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
          <>
            <VoucherBox
              voucherCode={payment.voucherCode}
              onInputChange={payment.handleInputChange}
              onApply={payment.handleApplyVoucher}
              loading={payment.loading}
              error={payment.error}
              voucherValid={payment.previewData?.voucherValid}
              voucherRequiresApply={payment.voucherRequiresApply}
              discountAmount={payment.amounts.discountAmount}
            />
            <CashTenderedPanel
              finalAmount={payment.amounts.finalAmount}
              cashReceived={payment.cashReceived}
              onChangeCashReceived={payment.setCashReceived}
            />
          </>
        )}

        <PaymentSummary {...payment.amounts} />
      </div>

      <PaymentActions
        paymentMethod={payment.paymentMethod}
        loading={payment.loading}
        cashDisabled={payment.voucherRequiresApply || payment.isInsufficient}
        cashDisabledReason={
          payment.isInsufficient
            ? 'Tiền khách đưa chưa đủ để thanh toán.'
            : payment.voucherRequiresApply
              ? 'Vui lòng áp dụng hoặc xóa mã voucher trước khi thanh toán.'
              : ''
        }
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
