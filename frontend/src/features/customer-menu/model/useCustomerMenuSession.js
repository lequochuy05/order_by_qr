import { useCallback, useEffect, useState } from 'react';

import { ORDER_STATUSES } from '@entities/order/lib/orderStatus.js';
import { publicMenuService } from '@entities/public-menu/api/publicMenuService.js';
import { isTerminalSessionError, useTableSessionQuery, useStartTableSessionMutation } from '@entities/table';
import { getSessionStorageKey } from '../lib/restaurantSettings.js';

const getSigStorageKey = (code) => `qros_qr_sig_${code}`;

const useCustomerMenuSession = (tableCode, { onSessionEnded, sig } = {}) => {
  const [sessionToken, setSessionToken] = useState(() =>
    tableCode ? sessionStorage.getItem(getSessionStorageKey(tableCode)) || '' : '',
  );
  const [paymentInProgress, setPaymentInProgress] = useState(false);
  const { mutateAsync: startTableSession } = useStartTableSessionMutation();
  const query = useTableSessionQuery(tableCode, sessionToken);

  useEffect(() => {
    if (tableCode && sig) {
      sessionStorage.setItem(getSigStorageKey(tableCode), sig);
    }
  }, [tableCode, sig]);

  const sessionEnded = query.data?.sessionEnded;
  const sessionError = query.data?.sessionError;
  const hasTerminalSessionError = isTerminalSessionError(sessionError);

  const persistSessionToken = useCallback(
    (token) => {
      if (!tableCode || !token) return;
      sessionStorage.setItem(getSessionStorageKey(tableCode), token);
      setSessionToken(token);
    },
    [tableCode],
  );

  const clearSessionToken = useCallback(() => {
    if (tableCode) {
      sessionStorage.removeItem(getSessionStorageKey(tableCode));
      sessionStorage.removeItem(getSigStorageKey(tableCode));
    }
    setSessionToken('');
  }, [tableCode]);

  const ensureSessionToken = useCallback(async () => {
    if (sessionToken) return sessionToken;
    if (!tableCode) {
      throw new Error('Vui lòng quét mã QR trên bàn để đặt món.');
    }

    const currentSig = sig || (tableCode ? sessionStorage.getItem(getSigStorageKey(tableCode)) : null);
    const startedSession = await startTableSession({ tableCode, sig: currentSig });
    persistSessionToken(startedSession.sessionToken);
    return startedSession.sessionToken;
  }, [persistSessionToken, sessionToken, startTableSession, tableCode, sig]);

  useEffect(() => {
    if (!sessionToken || (!sessionEnded && !hasTerminalSessionError)) return;
    const timeout = window.setTimeout(() => {
      clearSessionToken();
      onSessionEnded?.();
    }, 0);
    return () => window.clearTimeout(timeout);
  }, [clearSessionToken, hasTerminalSessionError, onSessionEnded, sessionEnded, sessionToken]);

  useEffect(() => {
    if (query.data?.currentOrder?.status === ORDER_STATUSES.AWAITING_PAYMENT) return;
    const timeout = window.setTimeout(() => setPaymentInProgress(false), 0);
    return () => window.clearTimeout(timeout);
  }, [query.data?.currentOrder?.status]);

  useEffect(() => {
    if (!sessionToken) return undefined;

    const sendHeartbeat = () => {
      if (document.hidden) return;

      publicMenuService.heartbeatSession(sessionToken).catch((error) => {
        if (isTerminalSessionError(error)) clearSessionToken();
      });
    };

    const timer = window.setInterval(sendHeartbeat, 90 * 1000);
    return () => window.clearInterval(timer);
  }, [clearSessionToken, sessionToken]);

  return {
    ...query,
    sessionToken,
    paymentInProgress,
    setPaymentInProgress,
    ensureSessionToken,
    clearSessionToken,
    hasTerminalSessionError,
  };
};

export default useCustomerMenuSession;
