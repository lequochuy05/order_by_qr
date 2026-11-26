import { useMemo } from 'react';

import usePaymentDraft from './usePaymentDraft.js';
import usePaymentPreview from './usePaymentPreview.js';
import usePayosPayment from './usePayosPayment.js';

const usePaymentModal = (props) => {
  const paymentDraft = usePaymentDraft(props.order?.id);
  const preview = usePaymentPreview({
    isOpen: props.isOpen,
    order: props.order,
    table: props.table,
    paymentDraft,
  });
  const payment = usePayosPayment({
    ...props,
    preview,
    paymentDraft,
  });

  const amounts = useMemo(() => {
    const subtotalAmount =
      preview.previewData?.subtotalAmount ??
      payment.payosData?.subtotalAmount ??
      preview.amounts.subtotalAmount;
    const discountAmount =
      preview.previewData?.discountAmount ??
      payment.payosData?.discountAmount ??
      preview.amounts.discountAmount;
    const discountPromotion =
      preview.previewData?.discountPromotion ??
      (!props.order?.voucherCode && !payment.payosData?.voucherCode
        ? discountAmount
        : preview.amounts.discountPromotion ?? 0);
    const voucherDiscountAmount = Math.max(0, discountAmount - discountPromotion);
    const finalAmount =
      preview.previewData?.finalAmount ??
      payment.payosData?.finalAmount ??
      payment.payosData?.amount ??
      preview.amounts.finalAmount;

    return {
      subtotalAmount,
      discountAmount,
      discountPromotion,
      voucherDiscountAmount,
      finalAmount,
    };
  }, [payment.payosData, preview.amounts, preview.previewData, props.order?.voucherCode]);

  return { ...preview, ...payment, amounts, error: preview.error || payment.payosError };
};

export default usePaymentModal;
