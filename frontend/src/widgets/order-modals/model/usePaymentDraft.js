import { useCallback, useMemo } from 'react';

const usePaymentDraft = (orderId) => {
  const draftKey = useMemo(() => (orderId ? `payment_draft_${orderId}` : null), [orderId]);

  const readPaymentDraft = useCallback(() => {
    if (!draftKey) return null;
    try {
      return JSON.parse(sessionStorage.getItem(draftKey));
    } catch {
      return null;
    }
  }, [draftKey]);

  const writePaymentDraft = useCallback(
    (nextDraft) => {
      if (!draftKey) return;
      const normalizedDraft = Object.fromEntries(
        Object.entries(nextDraft).filter(([, value]) => value !== undefined && value !== null),
      );
      if (Object.keys(normalizedDraft).length === 0) {
        sessionStorage.removeItem(draftKey);
        return;
      }
      sessionStorage.setItem(draftKey, JSON.stringify(normalizedDraft));
    },
    [draftKey],
  );

  const savePaymentDraft = useCallback(
    (voucherCode, previewData) => {
      if (!draftKey || !voucherCode || !previewData?.voucherValid) return;
      writePaymentDraft({ ...readPaymentDraft(), voucherCode, previewData });
    },
    [draftKey, readPaymentDraft, writePaymentDraft],
  );

  const savePayosDraft = useCallback(
    (payosData) => {
      if (!draftKey || !payosData?.transactionId || !payosData?.qrCode) return;
      writePaymentDraft({ ...readPaymentDraft(), payosData });
    },
    [draftKey, readPaymentDraft, writePaymentDraft],
  );

  const clearPayosDraft = useCallback(() => {
    const currentDraft = readPaymentDraft();
    if (!currentDraft) return;
    const nextDraft = { ...currentDraft };
    delete nextDraft.payosData;
    writePaymentDraft(nextDraft);
  }, [readPaymentDraft, writePaymentDraft]);

  const clearPaymentDraft = useCallback(() => {
    if (draftKey) sessionStorage.removeItem(draftKey);
  }, [draftKey]);

  return useMemo(
    () => ({
      savePaymentDraft,
      savePayosDraft,
      clearPayosDraft,
      clearPaymentDraft,
      readPaymentDraft,
    }),
    [clearPayosDraft, clearPaymentDraft, readPaymentDraft, savePaymentDraft, savePayosDraft],
  );
};

export default usePaymentDraft;
