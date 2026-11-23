import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { publicMenuService } from '@entities/public-menu/api/publicMenuService.js';
import { calculatePayosTimeLeft } from '@entities/payment/lib/paymentExpiry.js';
import { PAYMENT_TRANSACTION_STATUSES } from '@entities/payment/lib/paymentStatus.js';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import {
  calculateSelfCheckoutBreakdown,
  createSelfCheckoutWithRetry,
  getSelfCheckoutStorageKey,
  getVoucherValidationError,
  hasUnfinishedOrderItems,
} from '../lib/selfCheckoutHelpers.js';

const PAYOS_FIRST_SYNC_DELAY_MS = 2500;
const PAYOS_SYNC_INTERVAL_MS = 6000;

const createIdempotencyKey = () =>
  crypto.randomUUID
    ? crypto.randomUUID()
    : Math.random().toString(36).substring(2) + Date.now().toString(36);

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

const sameId = (left, right) =>
  left != null && right != null && String(left) === String(right);

const getPaymentEventOrderId = (message) =>
  message?.orderId ?? message?.id ?? message?.data?.orderId;

const getPaymentEventTransactionId = (message) =>
  message?.transactionId ?? message?.data?.transactionId;

const useCustomerSelfCheckout = ({
  tableCode,
  sessionToken,
  order,
  activePromotion = null,
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
  const [timeLeft, setTimeLeft] = useState(() => initialTimeLeft);

  const successHandledRef = useRef(false);
  const syncingRef = useRef(false);

  const unfinishedItems = useMemo(() => hasUnfinishedOrderItems(order), [order]);
  const canCreateQr = !unfinishedItems;

  const summary = useMemo(
    () => calculateSelfCheckoutBreakdown(order, appliedVoucher, activePromotion, payosData),
    [activePromotion, appliedVoucher, order, payosData],
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

  // Countdown timer while QR is waiting
  useEffect(() => {
    if (payosStatus !== 'waiting' || !payosData?.expiresAt) return undefined;

    const updateTimer = () => {
      const remaining = calculatePayosTimeLeft(payosData.expiresAt);
      setTimeLeft(remaining);
      if (remaining <= 0) {
        setPayosStatus('expired');
      }
    };
    updateTimer();
    const timerId = window.setInterval(updateTimer, 1000);
    return () => window.clearInterval(timerId);
  }, [payosData?.expiresAt, payosStatus]);

  // Real-time payment completion event on /topic/tables
  useWebSocket(
    payosStatus === 'waiting' ? '/topic/tables' : null,
    (message) => {
      if (message?.event !== 'PAYMENT_SUCCESS') return;
      const eventOrderId = getPaymentEventOrderId(message);
      const eventTransactionId = getPaymentEventTransactionId(message);
      if (
        sameId(eventOrderId, order?.id) ||
        sameId(eventTransactionId, payosData?.transactionId)
      ) {
        markPaymentSucceeded();
      }
    },
    { scope: 'public' },
  );

  // Periodic fallback status sync with PayOS gateway while waiting
  useEffect(() => {
    if (
      payosStatus !== 'waiting' ||
      !payosData?.transactionId ||
      !tableCode ||
      !sessionToken
    ) {
      return undefined;
    }

    let stopped = false;

    const syncStatus = async () => {
      if (syncingRef.current || successHandledRef.current) return;
      syncingRef.current = true;
      try {
        const tx = await publicMenuService.syncSelfCheckoutStatus(
          tableCode,
          payosData.transactionId,
          sessionToken,
        );
        if (stopped) return;

        if (tx?.status === PAYMENT_TRANSACTION_STATUSES.PAID) {
          markPaymentSucceeded();
        } else if (tx?.status === PAYMENT_TRANSACTION_STATUSES.EXPIRED) {
          setPayosStatus('expired');
        } else if (tx?.status === PAYMENT_TRANSACTION_STATUSES.CANCELLED) {
          setPayosStatus('idle');
          setPayosData(null);
          writeSelfCheckoutDraft(storageKey, null);
        }
      } catch {
        // Ignore transient sync failures; next interval or WS will handle
      } finally {
        syncingRef.current = false;
      }
    };

    const firstTimeout = window.setTimeout(syncStatus, PAYOS_FIRST_SYNC_DELAY_MS);
    const intervalId = window.setInterval(syncStatus, PAYOS_SYNC_INTERVAL_MS);
    return () => {
      stopped = true;
      window.clearTimeout(firstTimeout);
      window.clearInterval(intervalId);
    };
  }, [
    markPaymentSucceeded,
    payosData?.transactionId,
    payosStatus,
    sessionToken,
    storageKey,
    tableCode,
  ]);

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
      setTimeLeft(nextTimeLeft);
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
      setTimeLeft(0);
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
    timeLeft,
    handleVoucherInputChange,
    handleApplyVoucher,
    handleClearVoucher,
    handleCreateQr,
    handleCancelQr,
  };
};

export default useCustomerSelfCheckout;
