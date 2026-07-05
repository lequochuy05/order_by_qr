import { useCallback } from 'react';
import { paymentService } from '@entities/payment/api/paymentService.js';
import { PAYMENT_METHODS } from '@entities/payment/lib/paymentStatus.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { showErrorToast } from '@shared/lib/toast.js';

/**
 * Xử lý flow thanh toán tiền mặt.
 */
const useCashPayment = ({ cashPaymentEnabled, order, preview, table, finishPayment }) => {
  const { confirm } = useConfirmModal();
  const orderId = order?.id;
  const tableNumber = table?.tableNumber;

  const handleConfirmCashPay = useCallback(async () => {
    if (!cashPaymentEnabled) return;
    if (preview.voucherRequiresApply) return;
    const confirmed = await confirm(
      'Xác nhận thanh toán',
      `Xác nhận thanh toán TIỀN MẶT cho bàn ${tableNumber}?`,
    );
    if (!confirmed) return;

    try {
      const voucherCode = preview.payableVoucherCode;
      await paymentService.createPayment(orderId, PAYMENT_METHODS.CASH, voucherCode);
      await finishPayment(PAYMENT_METHODS.CASH);
    } catch (error) {
      showErrorToast(error);
    }
  }, [
    cashPaymentEnabled,
    confirm,
    finishPayment,
    orderId,
    preview.payableVoucherCode,
    preview.voucherRequiresApply,
    tableNumber,
  ]);

  return { handleConfirmCashPay };
};

export default useCashPayment;
