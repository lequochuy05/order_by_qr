import { useEffect, useRef } from 'react';
import wsService from '@shared/lib/websocket.js';

export const useWebSocket = (topic, onMessage) => {
  const subscriptionRef = useRef(null);
  const onMessageRef = useRef(onMessage);

  useEffect(() => {
    onMessageRef.current = onMessage;
  }, [onMessage]);

  useEffect(() => {
    if (!topic) {
      return undefined;
    }

    wsService.connect();

    const doSubscribe = () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
      }
      subscriptionRef.current = wsService.subscribe(topic, (msg) => {
        if (onMessageRef.current) {
          onMessageRef.current(msg);
        }
      });
    };

    const removeConnectListener = wsService.addConnectListener(doSubscribe);

    if (wsService.isConnected()) {
      doSubscribe();
    }

    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }
      removeConnectListener();
    };
  }, [topic]); // Cập nhật khi topic đổi

  return wsService;
};
