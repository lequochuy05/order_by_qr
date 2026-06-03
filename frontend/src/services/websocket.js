import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getAccessToken } from './api';

class WebSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
        this.onConnectCallbacks = [];
        this.statusListeners = new Set();
        this.subscriptions = new Set();
    }

    connect() {
        if (this.client?.active) return;
        const wsUrl = import.meta.env.VITE_WS_URL || '/ws';

        this.client = new Client({
            webSocketFactory: () => new SockJS(wsUrl),
            connectHeaders: () => this.getConnectHeaders(),
            reconnectDelay: 5000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            onConnect: () => {
                this.connected = true;
                this.notifyStatusChange();
                const callbacks = [...this.onConnectCallbacks];
                this.onConnectCallbacks = [];
                callbacks.forEach(cb => cb());
            },
            onDisconnect: () => {
                this.connected = false;
                this.notifyStatusChange();
            },
            onWebSocketClose: () => {
                this.connected = false;
                this.notifyStatusChange();
            },
            onStompError: (frame) => {
                console.error('WebSocket broker error:', frame.headers?.message || frame.body);
            }
        });
        this.client.activate();
    }

    isConnected() {
        return this.connected && Boolean(this.client?.connected);
    }

    notifyStatusChange() {
        const status = this.isConnected();
        this.statusListeners.forEach(listener => listener(status));
    }

    addConnectListener(listener) {
        if (typeof listener !== 'function') return () => {};
        this.statusListeners.add(listener);
        return () => this.statusListeners.delete(listener);
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

        if (this.isConnected()) {
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
        this.statusListeners.clear();
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
