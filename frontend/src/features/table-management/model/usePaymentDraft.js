import { useCallback, useMemo } from 'react';

const usePaymentDraft = (orderId) => {
  const draftKey = useMemo(() => (orderId ? `payment_draft_${orderId}` : null), [orderId]);

  const savePaymentDraft = useCallback(
    (voucherCode, previewData) => {
      if (!draftKey || !voucherCode || !previewData?.voucherValid) return;
      sessionStorage.setItem(draftKey, JSON.stringify({ voucherCode, previewData }));
    },
    [draftKey],
  );

  const clearPaymentDraft = useCallback(() => {
    if (draftKey) sessionStorage.removeItem(draftKey);
  }, [draftKey]);

  const readPaymentDraft = useCallback(() => {
    if (!draftKey) return null;
    try {
      return JSON.parse(sessionStorage.getItem(draftKey));
    } catch {
      return null;
    }
  }, [draftKey]);

  return useMemo(
    () => ({ savePaymentDraft, clearPaymentDraft, readPaymentDraft }),
    [clearPaymentDraft, readPaymentDraft, savePaymentDraft],
  );
};

export default usePaymentDraft;
