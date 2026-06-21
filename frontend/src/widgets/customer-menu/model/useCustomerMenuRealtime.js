import { useEffect, useState } from 'react';

import { queryClient } from '@shared/api/queryClient.js';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import wsService from '@shared/lib/websocket.js';

const useCustomerMenuRealtime = ({ tableCode, sessionToken }) => {
  const [wsConnected, setWsConnected] = useState(() => wsService.isConnected());

  useWebSocket(sessionToken ? '/topic/tables' : null, (message) => {
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
  });

  useEffect(() => {
    return wsService.addConnectListener((connected) => {
      setWsConnected(connected);
    });
  }, []);

  return wsConnected;
};

export default useCustomerMenuRealtime;
