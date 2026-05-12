package com.sacmauquan.qrordering.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import java.util.Objects;

/**
 * WebSocketEventListener - Listens for internal WebSocketEvent and pushes them to the messaging template.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles WebSocketEvent after the transaction has successfully committed.
     * Ensures that real-time notifications are only sent if the database changes are persistent.
     * 
     * @param event The triggered WebSocketEvent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleWebSocketEvent(WebSocketEvent event) {
        try {
            String destination = Objects.requireNonNull(event.destination());
            Object payload = Objects.requireNonNull(event.payload());

            messagingTemplate.convertAndSend(destination, payload);
            
            if (StringUtils.hasText(event.logMessage())) {
                log.debug("[WS Notification] Sent to {}: {}", destination, event.logMessage());
            }
        } catch (Exception e) {
            log.error("Critical error while sending WebSocket to {}: {}", event.destination(), e.getMessage());
        }
    }
}
