import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
        this.onConnectCallbacks = []; 
    }

    connect() {
        if (this.client) return;

        const wsUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';
        
        this.client = new Client({
            webSocketFactory: () => new SockJS(wsUrl),
            reconnectDelay: 5000,
            onConnect: () => {
                this.connected = true;
                console.log("WebSocket Connected");
                // Chạy toàn bộ hàng đợi subscribe
                this.onConnectCallbacks.forEach(cb => cb());
                this.onConnectCallbacks = []; 
            },
            onDisconnect: () => {
                this.connected = false;
                console.log("WebSocket Disconnected");
            }
        });
        this.client.activate();
    }

    subscribe(topic, callback) {
        if (this.connected && this.client?.connected) {
            return this.client.subscribe(topic, (message) => {
                try {
                    callback(JSON.parse(message.body));
                } catch (e) {
                    callback(message.body);
                }
            });
        } else {
            // Nếu chưa connect, đẩy vào hàng đợi để sub sau
            this.onConnectCallbacks.push(() => this.subscribe(topic, callback));
        }
    }
}

const wsService = new WebSocketService();
export default wsService;