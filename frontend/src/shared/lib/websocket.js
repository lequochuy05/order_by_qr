import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getAccessToken } from '@shared/api/httpClient.js';

class WebSocketService {
  constructor({ name = 'websocket', authenticated = false } = {}) {
    this.name = name;
    this.authenticated = authenticated;
    this.client = null;
    this.connected = false;
    this.onConnectCallbacks = [];
    this.statusListeners = new Set();
    this.subscriptions = new Set();
  }

  canConnect() {
    return !this.authenticated || Boolean(getAccessToken());
  }

  connect() {
    if (this.client?.active) return true;
    if (!this.canConnect()) {
      this.connected = false;
      this.notifyStatusChange();
      return false;
    }

    const wsUrl = import.meta.env.VITE_WS_URL || '/ws';

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: this.getConnectHeaders(),
      beforeConnect: (stompClient) => {
        if (!this.canConnect()) {
          throw new Error(`${this.name} WebSocket requires an access token`);
        }
        stompClient.connectHeaders = this.getConnectHeaders();
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        if (this.client !== client) return;
        this.connected = true;
        this.notifyStatusChange();
        const callbacks = [...this.onConnectCallbacks];
        this.onConnectCallbacks = [];
        callbacks.forEach((cb) => cb());
      },
      onDisconnect: () => {
        if (this.client !== client) return;
        this.connected = false;
        this.notifyStatusChange();
      },
      onWebSocketClose: () => {
        if (this.client !== client) return;
        this.connected = false;
        this.notifyStatusChange();
      },
      onStompError: (frame) => {
        if (this.client !== client) return;
        console.error(
          `${this.name} WebSocket broker error:`,
          frame.headers?.message || frame.body,
        );
      },
    });
    this.client = client;
    client.activate();
    return true;
  }

  reconnect() {
    this.disconnect({ clearQueued: false });
    return this.connect();
  }

  isConnected() {
    return this.connected && Boolean(this.client?.connected);
  }

  isActive() {
    return Boolean(this.client?.active);
  }

  notifyStatusChange() {
    const status = this.isConnected();
    this.statusListeners.forEach((listener) => listener(status));
  }

  addConnectListener(listener) {
    if (typeof listener !== 'function') return () => {};
    this.statusListeners.add(listener);
    return () => this.statusListeners.delete(listener);
  }

  getConnectHeaders() {
    if (!this.authenticated) return {};
    const token = getAccessToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  subscribe(topic, callback) {
    if (!topic || typeof callback !== 'function') return null;

    const subscribeNow = () => {
      if (!this.client?.connected) return null;
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
        this.onConnectCallbacks = this.onConnectCallbacks.filter((cb) => cb !== queuedSubscribe);
        if (innerSubscription) innerSubscription.unsubscribe();
      },
    };
  }

  disconnect({ clearQueued = true, clearListeners = false } = {}) {
    if (clearQueued) {
      this.onConnectCallbacks = [];
    }
    [...this.subscriptions].forEach((subscription) => subscription.unsubscribe());
    this.subscriptions.clear();
    this.connected = false;
    if (clearListeners) {
      this.statusListeners.clear();
    }
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    this.notifyStatusChange();
  }
}

export const publicWsService = new WebSocketService({ name: 'public', authenticated: false });
export const adminWsService = new WebSocketService({ name: 'admin', authenticated: true });

export default publicWsService;
