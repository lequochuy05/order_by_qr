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

  const amounts = useMemo(
    () => ({
      subtotalAmount:
        preview.previewData?.subtotalAmount ??
        payment.payosData?.subtotalAmount ??
        preview.amounts.subtotalAmount,
      discountAmount:
        preview.previewData?.discountAmount ??
        payment.payosData?.discountAmount ??
        preview.amounts.discountAmount,
      finalAmount:
        preview.previewData?.finalAmount ??
        payment.payosData?.finalAmount ??
        payment.payosData?.amount ??
        preview.amounts.finalAmount,
    }),
    [payment.payosData, preview.amounts, preview.previewData],
  );

  return { ...preview, ...payment, amounts, error: preview.error || payment.payosError };
};

export default usePaymentModal;
