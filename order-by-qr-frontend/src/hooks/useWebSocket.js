import { useEffect, useRef } from 'react';
import wsService from '../services/websocket';

export const useWebSocket = (topic, onMessage) => {
    const subscriptionRef = useRef(null);
    const onMessageRef = useRef(onMessage);

    useEffect(() => {
        onMessageRef.current = onMessage;
    }, [onMessage]);

    useEffect(() => {
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

        if (wsService.connected) {
            doSubscribe();
        } else {
            wsService.onConnectCallbacks.push(doSubscribe);
        }

        return () => {
            if (subscriptionRef.current) {
                subscriptionRef.current.unsubscribe();
                subscriptionRef.current = null;
            }
            wsService.onConnectCallbacks = wsService.onConnectCallbacks.filter(cb => cb !== doSubscribe);
        };
    }, [topic]); // Cập nhật khi topic đổi

    return wsService;
};