import { useCallback, useState } from 'react';
import { paymentService } from '@entities/payment/api/paymentService.js';
import { PAYMENT_METHODS } from '@entities/payment/lib/paymentStatus.js';
import { ORDER_PAYMENT_STATUSES } from '@entities/order/lib/orderStatus.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { showErrorToast } from '@shared/lib/toast.js';
import { fmtVND } from '@shared/lib/formatters.js';

/**
 * Xử lý flow thanh toán tiền mặt với tính tiền thừa & kiểm tra món bếp dở dang.
 */
const useCashPayment = ({
  cashPaymentEnabled,
  order,
  preview,
  table,
  finishPayment,
  finalAmount = 0,
}) => {
  const { confirm } = useConfirmModal();
  const orderId = order?.id;
  const tableNumber = table?.tableNumber;

  const [cashReceived, setCashReceived] = useState('');

  const numericCash = Number(cashReceived) || 0;
  const isInsufficient = numericCash > 0 && numericCash < finalAmount;
  const changeAmount = Math.max(0, (numericCash || finalAmount) - finalAmount);

  // Check if any items are still being prepared in kitchen
  const orderItems = order?.orderItems || order?.items || [];
  const unfinishedItems = orderItems.filter(
    (item) => item.status === 'PENDING' || item.status === 'COOKING',
  );
  const hasUnfinishedItems = unfinishedItems.length > 0;

  const handleConfirmCashPay = useCallback(async () => {
    if (!cashPaymentEnabled) return;
    if (preview.voucherRequiresApply) return;
    if (isInsufficient) {
      showErrorToast(new Error('Số tiền khách đưa chưa đủ để thanh toán.'));
      return;
    }

    let confirmUnfinished = false;
    if (hasUnfinishedItems) {
      const unfinishedNames = unfinishedItems
        .map((i) => i.itemNameSnapshot || i.menuItem?.name || 'Món')
        .slice(0, 3)
        .join(', ');
      const suffix = unfinishedItems.length > 3 ? ` và ${unfinishedItems.length - 3} món khác` : '';

      const proceedWithUnfinished = await confirm(
        'Bếp vẫn đang chế biến!',
        `Bàn ${tableNumber} còn ${unfinishedItems.length} món chưa hoàn thành (${unfinishedNames}${suffix}).\n\nBạn có chắc chắn muốn thanh toán trước không?`,
      );
      if (!proceedWithUnfinished) return;
      confirmUnfinished = true;
    } else {
      const promptText =
        numericCash > finalAmount
          ? `Xác nhận thanh toán TIỀN MẶT cho bàn ${tableNumber}?\nTiền khách đưa: ${fmtVND(numericCash)}\nTiền thừa trả khách: ${fmtVND(changeAmount)}`
          : `Xác nhận thanh toán TIỀN MẶT cho bàn ${tableNumber}?`;

      const confirmed = await confirm('Xác nhận thanh toán', promptText);
      if (!confirmed) return;
    }

    try {
      const voucherCode = preview.payableVoucherCode;
      const actualCash = numericCash > 0 ? numericCash : finalAmount;
      const actualChange = Math.max(0, actualCash - finalAmount);

      const payment = await paymentService.createPayment(
        orderId,
        PAYMENT_METHODS.CASH,
        voucherCode,
        {
          cashReceived: actualCash,
          changeAmount: actualChange,
          confirmUnfinishedItems: confirmUnfinished,
        },
      );

      await finishPayment(
        PAYMENT_METHODS.CASH,
        {
          ...order,
          id: payment?.orderId ?? orderId,
          subtotalAmount: payment?.subtotalAmount ?? order?.subtotalAmount,
          discountAmount: payment?.discountAmount ?? order?.discountAmount,
          finalAmount: payment?.finalAmount ?? payment?.amount ?? order?.finalAmount,
          voucherCode: payment?.voucherCode ?? voucherCode ?? order?.voucherCode,
          paymentMethod: PAYMENT_METHODS.CASH,
          paymentStatus: ORDER_PAYMENT_STATUSES.PAID,
          paymentTime: payment?.createdAt ?? new Date().toISOString(),
        },
        { skipReconcile: true },
      );
    } catch (error) {
      showErrorToast(error);
    }
  }, [
    cashPaymentEnabled,
    changeAmount,
    confirm,
    finalAmount,
    finishPayment,
    hasUnfinishedItems,
    isInsufficient,
    numericCash,
    order,
    orderId,
    preview.payableVoucherCode,
    preview.voucherRequiresApply,
    tableNumber,
    unfinishedItems,
  ]);

  return {
    cashReceived,
    setCashReceived,
    changeAmount,
    isInsufficient,
    hasUnfinishedItems,
    handleConfirmCashPay,
  };
};

export default useCashPayment;
