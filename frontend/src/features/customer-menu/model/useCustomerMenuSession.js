import { useCallback, useEffect, useState } from 'react';

import { publicMenuService } from '@entities/public-menu/api/publicMenuService.js';
import { isTerminalSessionError, useTableSessionQuery, useStartTableSessionMutation } from '@entities/table';
import { getSessionStorageKey } from '../lib/restaurantSettings.js';

const useCustomerMenuSession = (tableCode, { onSessionEnded } = {}) => {
  const [sessionToken, setSessionToken] = useState(() =>
    tableCode ? sessionStorage.getItem(getSessionStorageKey(tableCode)) || '' : '',
  );
  const [paymentInProgress, setPaymentInProgress] = useState(false);
  const { mutateAsync: startTableSession } = useStartTableSessionMutation();
  const query = useTableSessionQuery(tableCode, sessionToken);

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
    }
    setSessionToken('');
  }, [tableCode]);

  const ensureSessionToken = useCallback(async () => {
    if (sessionToken) return sessionToken;
    if (!tableCode) {
      throw new Error('Vui lòng quét mã QR trên bàn để đặt món.');
    }

    const startedSession = await startTableSession(tableCode);
    persistSessionToken(startedSession.sessionToken);
    return startedSession.sessionToken;
  }, [persistSessionToken, sessionToken, startTableSession, tableCode]);

  useEffect(() => {
    if (!sessionToken || (!sessionEnded && !hasTerminalSessionError)) return;
    const timeout = window.setTimeout(() => {
      clearSessionToken();
      onSessionEnded?.();
    }, 0);
    return () => window.clearTimeout(timeout);
  }, [clearSessionToken, hasTerminalSessionError, onSessionEnded, sessionEnded, sessionToken]);

  useEffect(() => {
    if (query.data?.currentOrder?.status === 'AWAITING_PAYMENT') return;
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
