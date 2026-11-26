import { useEffect, useRef } from 'react';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { PAYMENT_TRANSACTION_STATUSES } from '../lib/paymentStatus.js';
import {
  getPaymentEventOrderId,
  getPaymentEventTransactionId,
  sameId,
} from '../lib/paymentEventMatchers.js';

/**
 * Shared PayOS transaction monitor combining:
 * 1. Real-time WebSocket `PAYMENT_SUCCESS` listener on `/topic/tables`
 * 2. Periodic fallback gateway polling with in-flight deduplication
 *
 * Used by both cashier (`usePayosPayment`) and customer self-checkout (`useCustomerSelfCheckout`).
 */
export const usePayosTransactionMonitor = ({
  isWaiting = false,
  subscribeWebSocket = isWaiting,
  orderId,
  transactionId,
  wsScope = 'public',
  firstSyncDelayMs = 2500,
  syncIntervalMs = 6000,
  syncTransaction,
  shouldSkipSync,
  onPaid,
  onExpired,
  onCancelled,
  onSyncError,
}) => {
  const syncingRef = useRef(false);
  const handlersRef = useRef({
    syncTransaction,
    shouldSkipSync,
    onPaid,
    onExpired,
    onCancelled,
    onSyncError,
  });

  useEffect(() => {
    handlersRef.current = {
      syncTransaction,
      shouldSkipSync,
      onPaid,
      onExpired,
      onCancelled,
      onSyncError,
    };
  }, [onCancelled, onExpired, onPaid, onSyncError, shouldSkipSync, syncTransaction]);

  useWebSocket(
    subscribeWebSocket ? '/topic/tables' : null,
    (message) => {
      if (message?.event !== 'PAYMENT_SUCCESS') return;
      const eventOrderId = getPaymentEventOrderId(message);
      const eventTransactionId = getPaymentEventTransactionId(message);
      if (sameId(eventOrderId, orderId) || sameId(eventTransactionId, transactionId)) {
        handlersRef.current.onPaid?.();
      }
    },
    { scope: wsScope },
  );

  useEffect(() => {
    if (!isWaiting || !transactionId || !handlersRef.current.syncTransaction) {
      return undefined;
    }

    let stopped = false;

    const pollStatus = async () => {
      if (syncingRef.current || handlersRef.current.shouldSkipSync?.()) return;
      syncingRef.current = true;
      try {
        const tx = await handlersRef.current.syncTransaction(transactionId);
        if (stopped) return;

        if (tx?.status === PAYMENT_TRANSACTION_STATUSES.PAID) {
          handlersRef.current.onPaid?.();
        } else if (tx?.status === PAYMENT_TRANSACTION_STATUSES.EXPIRED) {
          handlersRef.current.onExpired?.();
        } else if (tx?.status === PAYMENT_TRANSACTION_STATUSES.CANCELLED) {
          handlersRef.current.onCancelled?.();
        }
      } catch (error) {
        handlersRef.current.onSyncError?.(error);
      } finally {
        syncingRef.current = false;
      }
    };

    const firstTimeout = window.setTimeout(pollStatus, firstSyncDelayMs);
    const intervalId = window.setInterval(pollStatus, syncIntervalMs);
    return () => {
      stopped = true;
      window.clearTimeout(firstTimeout);
      window.clearInterval(intervalId);
    };
  }, [firstSyncDelayMs, isWaiting, syncIntervalMs, transactionId]);
};
