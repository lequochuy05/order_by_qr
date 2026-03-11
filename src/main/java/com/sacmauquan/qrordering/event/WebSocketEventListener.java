package com.sacmauquan.qrordering.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWebSocketEvent(WebSocketEvent event) {
        try {
            messagingTemplate.convertAndSend(event.destination(), event.payload());
            if (StringUtils.hasText(event.logMessage())) {
                log.info(event.logMessage());
            }
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo WebSocket: {}", e.getMessage());
        }
    }
}
