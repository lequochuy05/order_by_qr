import { useEffect, useState } from 'react';

import { queryClient } from '@shared/api/queryClient.js';
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
