import { useEffect, useRef } from 'react';
import wsService from '../services/websocket';

export const useWebSocket = (topic, onMessage) => {
    const subscriptionRef = useRef(null);

    useEffect(() => {
        wsService.connect();

        const doSubscribe = () => {
            if (subscriptionRef.current) {
                subscriptionRef.current.unsubscribe();
            }
            subscriptionRef.current = wsService.subscribe(topic, onMessage);
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
        };
    }, [topic, onMessage]); // Cập nhật khi topic hoặc callback đổi

    return wsService;
};