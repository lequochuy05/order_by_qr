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
 * WebSocketEventListener - Lắng nghe sự kiện nội bộ và đẩy ra WebSocket.
*/
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * CHỈ gửi WebSocket khi Transaction chứa sự kiện đã COMMIT thành công.
     * fallbackExecution = true: Đảm bảo vẫn gửi được nếu gọi ngoài Transaction.
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
            log.error("Lỗi nghiêm trọng khi gửi WebSocket tới {}: {}", event.destination(), e.getMessage());
        }
    }
}
