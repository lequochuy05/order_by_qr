import { useEffect, useRef } from 'react';
import { adminWsService, publicWsService } from '@shared/lib/websocket.js';

const resolveWebSocketService = ({ service, scope } = {}) => {
  if (service) return service;
  return scope === 'admin' ? adminWsService : publicWsService;
};

export const useWebSocket = (topic, onMessage, options = {}) => {
  const subscriptionRef = useRef(null);
  const onMessageRef = useRef(onMessage);
  const service = resolveWebSocketService(options);

  useEffect(() => {
    onMessageRef.current = onMessage;
  }, [onMessage]);

  useEffect(() => {
    if (!topic) {
      return undefined;
    }

    service.connect();

    const doSubscribe = () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
      }
      subscriptionRef.current = service.subscribe(topic, (msg) => {
        if (onMessageRef.current) {
          onMessageRef.current(msg);
        }
      });
    };

    const removeConnectListener = service.addConnectListener((connected) => {
      if (connected) doSubscribe();
    });

    if (service.isConnected()) {
      doSubscribe();
    }

    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }
      removeConnectListener();
    };
  }, [service, topic]); // Cập nhật khi topic hoặc WebSocket service đổi

  return service;
};
