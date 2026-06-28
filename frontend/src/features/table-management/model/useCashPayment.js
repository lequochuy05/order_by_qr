import { useCallback } from 'react';
import { paymentService } from '@entities/payment/api/paymentService.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { showErrorToast } from '@shared/lib/toast.js';

/**
 * Xử lý flow thanh toán tiền mặt.
 */
const useCashPayment = ({ cashPaymentEnabled, order, preview, table, finishPayment }) => {
  const { confirm } = useConfirmModal();

  const handleConfirmCashPay = useCallback(async () => {
    if (!cashPaymentEnabled) return;
    const confirmed = await confirm(
      'Xác nhận thanh toán',
      `Xác nhận thanh toán TIỀN MẶT cho bàn ${table.tableNumber}?`,
    );
    if (!confirmed) return;

    try {
      const voucherCode = preview.voucherCode.trim() || null;
      await paymentService.createPayment(order.id, 'CASH', voucherCode);
      await finishPayment('CASH');
    } catch (error) {
      showErrorToast(error);
    }
  }, [cashPaymentEnabled, confirm, finishPayment, order?.id, preview.voucherCode, table?.tableNumber]);

  return { handleConfirmCashPay };
};

export default useCashPayment;
