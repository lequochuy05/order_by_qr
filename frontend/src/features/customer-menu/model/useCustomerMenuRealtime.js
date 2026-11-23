import { useEffect, useState } from 'react';

import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { publicWsService } from '@shared/lib/websocket.js';

const useCustomerMenuRealtime = ({ tableCode, sessionToken }) => {
  const [wsConnected, setWsConnected] = useState(() => publicWsService.isConnected());

  // H2 fix: Connect unconditionally so wsConnected is accurate for ALL customers,
  // not just those who already have a sessionToken. Without this, 100% of new
  // visitors see the "Ngoại tuyến" badge even when the server is reachable.
  useEffect(() => {
    publicWsService.connect();
  }, []);

  useWebSocket(
    sessionToken ? '/topic/tables' : null,
    (message) => {
      if (message?.event === 'SERVICE_REQUEST_UPDATED') {
        queryClient.invalidateQueries({
          queryKey: queryKeys.serviceRequests.customer(tableCode),
        });
        return;
      }

      if (
        message !== 'UPDATED' &&
        message?.event !== 'UPDATED' &&
        message?.event !== 'PAYMENT_SUCCESS'
      ) {
        return;
      }

      const queryKey = ['tableSession', tableCode, sessionToken];
      const queryState = queryClient.getQueryState(queryKey);
      const recentlyUpdated =
        queryState?.dataUpdatedAt && Date.now() - queryState.dataUpdatedAt < 1000;

      if (queryState?.fetchStatus === 'fetching' || recentlyUpdated) return;
      queryClient.invalidateQueries({ queryKey, exact: true });
      queryClient.invalidateQueries({
        queryKey: queryKeys.serviceRequests.customer(tableCode),
      });
    },
    { scope: 'public' },
  );

  useWebSocket(
    '/topic/menu',
    (message) => {
      if (message !== 'UPDATED' && (typeof message !== 'object' || message === null)) return;
      queryClient.invalidateQueries({ queryKey: queryKeys.menu.customer });
      if (tableCode && sessionToken) {
        queryClient.invalidateQueries({
          queryKey: ['tableSession', tableCode, sessionToken],
          exact: true,
        });
      }
    },
    { scope: 'public' },
  );

  useEffect(() => {
    return publicWsService.addConnectListener((connected) => {
      setWsConnected(connected);
    });
  }, []);

  return wsConnected;
};

export default useCustomerMenuRealtime;
