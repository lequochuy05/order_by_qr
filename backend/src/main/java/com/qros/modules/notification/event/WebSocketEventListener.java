package com.qros.modules.notification.event;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

/**
 * WebSocketEventListener - Listens for internal WebSocketEvent and pushes them to the messaging template.
 * Includes retry with exponential backoff for transient broker failures.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 100;

    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;

    private Counter sentCounter;
    private Counter failedCounter;

    @PostConstruct
    void initCounters() {
        sentCounter = Counter.builder("websocket.messages.sent")
                .description("WebSocket messages successfully delivered")
                .register(meterRegistry);
        failedCounter = Counter.builder("websocket.messages.failed")
                .description("WebSocket messages that failed after all retries")
                .register(meterRegistry);
    }

    /**
     * Handles WebSocketEvent after the transaction has successfully committed.
     * Ensures that real-time notifications are only sent if the database changes are persistent.
     * Retries up to {@value #MAX_RETRIES} times with exponential backoff on transient failures.
     *
     * @param event The triggered WebSocketEvent
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleWebSocketEvent(WebSocketEvent event) {
        String destination;
        Object payload;
        try {
            destination = Objects.requireNonNull(event.destination());
            payload = Objects.requireNonNull(event.payload());
        } catch (NullPointerException e) {
            log.error("[WS Notification] Event with null destination or payload, skipping");
            return;
        }

        sendWithRetry(destination, payload, event.logMessage());
    }

    /**
     * Attempts to send the WebSocket message with retry and exponential backoff.
     */
    private void sendWithRetry(String destination, Object payload, String logMessage) {
        Throwable lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                messagingTemplate.convertAndSend(destination, payload);

                if (StringUtils.hasText(logMessage)) {
                    log.debug("[WS Notification] Sent to {}: {}", destination, logMessage);
                }
                sentCounter.increment();
                return; // success
            } catch (Throwable e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    long delay = BASE_DELAY_MS * (1L << (attempt - 1)); // 100ms, 200ms, 400ms
                    log.warn(
                            "[WS Notification] Retry {}/{} after send failure to {}: {} (retrying in {}ms)",
                            attempt,
                            MAX_RETRIES,
                            destination,
                            e.getMessage(),
                            delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("[WS Notification] Interrupted during retry backoff for {}", destination);
                        return;
                    }
                }
            }
        }

        // All retries exhausted
        failedCounter.increment();
        log.error(
                "[WS Notification] Failed to send to {} after {} attempts: {}",
                destination,
                MAX_RETRIES,
                lastException != null ? lastException.getMessage() : "unknown");
    }
}
