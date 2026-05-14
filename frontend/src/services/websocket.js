import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getAccessToken } from './api';

class WebSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
        this.onConnectCallbacks = [];
        this.subscriptions = new Set();
    }

    connect() {
        if (this.client?.active) return;
        const wsUrl = import.meta.env.VITE_WS_URL;
        if (!wsUrl) {
            console.warn('VITE_WS_URL is not configured');
            return;
        }

        this.client = new Client({
            webSocketFactory: () => new SockJS(wsUrl),
            connectHeaders: this.getConnectHeaders(),
            reconnectDelay: 5000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            onConnect: () => {
                this.connected = true;
                const callbacks = [...this.onConnectCallbacks];
                this.onConnectCallbacks = [];
                callbacks.forEach(cb => cb());
            },
            onDisconnect: () => {
                this.connected = false;
            },
            onWebSocketClose: () => {
                this.connected = false;
            },
            onStompError: (frame) => {
                console.error('WebSocket broker error:', frame.headers?.message || frame.body);
            }
        });
        this.client.activate();
    }

    getConnectHeaders() {
        const token = getAccessToken();
        return token ? { Authorization: `Bearer ${token}` } : {};
    }

    subscribe(topic, callback) {
        if (!topic || typeof callback !== 'function') return null;

        const subscribeNow = () => {
            const subscription = this.client.subscribe(topic, (message) => {
                try {
                    callback(JSON.parse(message.body));
                } catch {
                    callback(message.body);
                }
            });

            this.subscriptions.add(subscription);
            const originalUnsubscribe = subscription.unsubscribe.bind(subscription);
            subscription.unsubscribe = () => {
                this.subscriptions.delete(subscription);
                originalUnsubscribe();
            };
            return subscription;
        };

        if (this.connected && this.client?.connected) {
            return subscribeNow();
        }

        let active = true;
        let innerSubscription = null;
        const queuedSubscribe = () => {
            if (active) innerSubscription = subscribeNow();
        };
        this.onConnectCallbacks.push(queuedSubscribe);

        return {
            unsubscribe: () => {
                active = false;
                this.onConnectCallbacks = this.onConnectCallbacks.filter(cb => cb !== queuedSubscribe);
                if (innerSubscription) innerSubscription.unsubscribe();
            }
        };
    }

    disconnect() {
        this.onConnectCallbacks = [];
        this.subscriptions.forEach(subscription => subscription.unsubscribe());
        this.subscriptions.clear();
        this.connected = false;
        if (this.client) {
            this.client.deactivate();
            this.client = null;
        }
    }
}

const wsService = new WebSocketService();
export default wsService;
