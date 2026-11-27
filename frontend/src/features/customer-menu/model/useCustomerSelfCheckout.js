import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { publicMenuService } from '@entities/public-menu';
import {
  PAYMENT_TRANSACTION_STATUSES,
  calculatePayosTimeLeft,
  usePayosTransactionMonitor,
} from '@entities/payment';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { createIdempotencyKey } from '@shared/lib/idempotency.js';
import {
  calculateSelfCheckoutBreakdown,
  createSelfCheckoutWithRetry,
  getSelfCheckoutStorageKey,
  getVoucherValidationError,
  hasUnfinishedOrderItems,
} from '../lib/selfCheckoutHelpers.js';

const PAYOS_FIRST_SYNC_DELAY_MS = 2500;
const PAYOS_SYNC_INTERVAL_MS = 6000;

const readSelfCheckoutDraft = (storageKey) => {
  if (!storageKey) return null;
  try {
    const raw = sessionStorage.getItem(storageKey);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

const writeSelfCheckoutDraft = (storageKey, data) => {
  if (!storageKey) return;
  try {
    if (!data) {
      sessionStorage.removeItem(storageKey);
      return;
    }
    sessionStorage.setItem(storageKey, JSON.stringify(data));
  } catch {
    // Ignore storage quota / privacy mode failures
  }
};

const useCustomerSelfCheckout = ({
  tableCode,
  sessionToken,
  order,
  activePromotion = null,
  restaurantSettings = null,
  onPaymentSuccess,
}) => {
  const storageKey = useMemo(
    () => getSelfCheckoutStorageKey(tableCode, order?.id),
    [order?.id, tableCode],
  );
  const initialDraft = useMemo(() => readSelfCheckoutDraft(storageKey), [storageKey]);
  const initialHasActiveQr = Boolean(
    initialDraft?.transactionId && initialDraft?.qrCode,
  );
  const initialTimeLeft = initialHasActiveQr
    ? calculatePayosTimeLeft(initialDraft.expiresAt)
    : 0;

  const [isCheckoutOpen, setIsCheckoutOpen] = useState(() => initialHasActiveQr);
  const [voucherCode, setVoucherCode] = useState(
    () => initialDraft?.voucherCode || order?.voucherCode || '',
  );
  const [appliedVoucher, setAppliedVoucher] = useState(() =>
    initialDraft?.appliedVoucher?.applicable ? initialDraft.appliedVoucher : null,
  );
  const [voucherLoading, setVoucherLoading] = useState(false);
  const [voucherError, setVoucherError] = useState('');

  const [payosData, setPayosData] = useState(() =>
    initialHasActiveQr ? initialDraft : null,
  );
  const [payosStatus, setPayosStatus] = useState(() =>
    initialHasActiveQr ? (initialTimeLeft > 0 ? 'waiting' : 'expired') : 'idle',
  );
  const [payosLoading, setPayosLoading] = useState(false);
  const [payosError, setPayosError] = useState('');

  const successHandledRef = useRef(false);
  const activeStorageKeyRef = useRef(storageKey);

  // Sync/reset draft cleanly when the active order changes while keeping monitor alive across sheet toggle
  useEffect(() => {
    if (activeStorageKeyRef.current === storageKey) return;
    activeStorageKeyRef.current = storageKey;
    successHandledRef.current = false;

    const draft = readSelfCheckoutDraft(storageKey);
    const hasActiveQr = Boolean(draft?.transactionId && draft?.qrCode);
    const timeLeft = hasActiveQr ? calculatePayosTimeLeft(draft.expiresAt) : 0;

    setIsCheckoutOpen(hasActiveQr);
    setVoucherCode(draft?.voucherCode || order?.voucherCode || '');
    setAppliedVoucher(draft?.appliedVoucher?.applicable ? draft.appliedVoucher : null);
    setVoucherError('');
    setPayosData(hasActiveQr ? draft : null);
    setPayosStatus(hasActiveQr ? (timeLeft > 0 ? 'waiting' : 'expired') : 'idle');
    setPayosError('');
  }, [order?.voucherCode, storageKey]);

  const unfinishedItems = useMemo(() => hasUnfinishedOrderItems(order), [order]);
  const canCreateQr = !unfinishedItems;

  const summary = useMemo(
    () =>
      calculateSelfCheckoutBreakdown(
        order,
        appliedVoucher,
        activePromotion,
        payosData,
        restaurantSettings,
      ),
    [activePromotion, appliedVoucher, order, payosData, restaurantSettings],
  );

  const normalizedInputCode = voucherCode.trim().toUpperCase();
  const normalizedAppliedCode = (
    appliedVoucher?.code ||
    (appliedVoucher ? '' : order?.voucherCode || '')
  )
    .trim()
    .toUpperCase();
  const voucherRequiresApply =
    normalizedInputCode.length > 0 && normalizedInputCode !== normalizedAppliedCode;
  const activeVoucherCode =
    !voucherRequiresApply && (appliedVoucher?.applicable || order?.voucherCode)
      ? normalizedAppliedCode || null
      : null;

  const markPaymentSucceeded = useCallback(() => {
    if (successHandledRef.current) return;
    successHandledRef.current = true;
    setPayosStatus('success');
    writeSelfCheckoutDraft(storageKey, null);
    onPaymentSuccess?.();
  }, [onPaymentSuccess, storageKey]);

  const handlePayosExpired = useCallback(() => {
    setPayosStatus('expired');
  }, []);

  const handleRemoteCancelled = useCallback(() => {
    setPayosStatus('idle');
    setPayosData(null);
    writeSelfCheckoutDraft(storageKey, null);
  }, [storageKey]);

  usePayosTransactionMonitor({
    isWaiting: payosStatus === 'waiting' && Boolean(tableCode) && Boolean(sessionToken),
    subscribeWebSocket: payosStatus === 'waiting',
    orderId: order?.id,
    transactionId: payosData?.transactionId,
    wsScope: 'public',
    firstSyncDelayMs: PAYOS_FIRST_SYNC_DELAY_MS,
    syncIntervalMs: PAYOS_SYNC_INTERVAL_MS,
    syncTransaction: (txId) =>
      publicMenuService.syncSelfCheckoutStatus(tableCode, txId, sessionToken),
    shouldSkipSync: () => successHandledRef.current,
    onPaid: markPaymentSucceeded,
    onExpired: handlePayosExpired,
    onCancelled: handleRemoteCancelled,
  });

  const handleVoucherInputChange = useCallback((value) => {
    setVoucherCode(value);
    setVoucherError('');
    if (!value.trim()) {
      setAppliedVoucher(null);
    }
  }, []);

  const handleApplyVoucher = useCallback(async () => {
    const code = voucherCode.trim().toUpperCase();
    if (!code || voucherLoading) return;

    setVoucherLoading(true);
    setVoucherError('');
    try {
      const result = await publicMenuService.validateVoucher(code, summary.subtotalAmount);
      if (result?.applicable) {
        setAppliedVoucher(result);
        setVoucherCode(result.code || code);
      } else {
        setAppliedVoucher(null);
        setVoucherError(getVoucherValidationError(result));
      }
    } catch (error) {
      setAppliedVoucher(null);
      setVoucherError(buildErrorMessage(error, { includeDetails: false }));
    } finally {
      setVoucherLoading(false);
    }
  }, [summary.subtotalAmount, voucherCode, voucherLoading]);

  const handleClearVoucher = useCallback(() => {
    setVoucherCode('');
    setAppliedVoucher(null);
    setVoucherError('');
  }, []);

  const handleCreateQr = useCallback(async () => {
    if (!tableCode || !sessionToken || !order?.id || payosLoading) return;
    if (!canCreateQr) {
      setPayosError('Bếp đang chế biến món, vui lòng chờ hoàn tất trước khi tạo mã.');
      setPayosStatus('error');
      return;
    }
    if (voucherRequiresApply) {
      setPayosError('Vui lòng bấm "Áp dụng" hoặc xóa mã giảm giá trước khi tạo mã QR.');
      setPayosStatus('error');
      return;
    }

    setPayosLoading(true);
    setPayosError('');
    try {
      const data = await createSelfCheckoutWithRetry({
        createFn: publicMenuService.createSelfCheckout,
        tableCode,
        sessionToken,
        voucherCode: activeVoucherCode,
        idempotencyKey: createIdempotencyKey(),
      });

      if (data?.status === PAYMENT_TRANSACTION_STATUSES.PAID) {
        markPaymentSucceeded();
        return;
      }

      const nextTimeLeft = calculatePayosTimeLeft(data?.expiresAt);
      const draftPayload = {
        ...data,
        voucherCode: data?.voucherCode || activeVoucherCode,
        appliedVoucher: appliedVoucher?.applicable
          ? appliedVoucher
          : data?.voucherCode && data?.discountAmount != null
            ? {
                code: data.voucherCode,
                appliedDiscountAmount: Math.max(
                  0,
                  Number(data.discountAmount) - summary.promotionDiscountAmount,
                ),
                finalAmount: data.finalAmount ?? data.amount,
                applicable: true,
              }
            : null,
      };
      setPayosData(data);
      setPayosStatus(nextTimeLeft > 0 ? 'waiting' : 'expired');
      writeSelfCheckoutDraft(storageKey, draftPayload);
    } catch (error) {
      setPayosError(buildErrorMessage(error, { includeDetails: false }));
      setPayosStatus('error');
    } finally {
      setPayosLoading(false);
    }
  }, [
    activeVoucherCode,
    appliedVoucher,
    canCreateQr,
    markPaymentSucceeded,
    order?.id,
    payosLoading,
    sessionToken,
    storageKey,
    summary.promotionDiscountAmount,
    tableCode,
    voucherRequiresApply,
  ]);

  const handleCancelQr = useCallback(async () => {
    if (!tableCode || !sessionToken || !payosData?.transactionId || payosLoading) return;
    setPayosLoading(true);
    setPayosError('');
    try {
      await publicMenuService.cancelSelfCheckout(
        tableCode,
        payosData.transactionId,
        {
          sessionToken,
          reason: 'Customer changed voucher or cancelled self-checkout QR',
        },
        sessionToken,
      );
      setPayosData(null);
      setPayosStatus('idle');
      writeSelfCheckoutDraft(storageKey, null);
    } catch (error) {
      setPayosError(buildErrorMessage(error, { includeDetails: false }));
    } finally {
      setPayosLoading(false);
    }
  }, [payosData?.transactionId, payosLoading, sessionToken, storageKey, tableCode]);

  return {
    isCheckoutOpen,
    setIsCheckoutOpen,
    voucherCode,
    appliedVoucher,
    voucherLoading,
    voucherError,
    voucherRequiresApply,
    summary,
    unfinishedItems,
    canCreateQr,
    payosData,
    payosStatus,
    payosLoading,
    payosError,
    handleVoucherInputChange,
    handleApplyVoucher,
    handleClearVoucher,
    handleCreateQr,
    handleCancelQr,
    handlePayosExpired,
  };
};

export default useCustomerSelfCheckout;
