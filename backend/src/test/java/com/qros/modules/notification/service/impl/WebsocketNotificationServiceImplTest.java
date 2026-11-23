package com.qros.modules.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.qros.modules.notification.event.WebSocketEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class WebsocketNotificationServiceImplTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WebsocketNotificationServiceImpl notificationService;

    @Test
    void notifyPromotionChangePublishesToPromotionsAndPublicMenuTopics() {
        notificationService.notifyPromotionChange();

        ArgumentCaptor<WebSocketEvent> captor = ArgumentCaptor.forClass(WebSocketEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(WebSocketEvent::destination)
                .containsExactly("/topic/promotions", "/topic/menu");
    }

    @Test
    void notifyMenuChangePublishesToMenuAndCombosTopics() {
        notificationService.notifyMenuChange("availability_updated", 10L);

        ArgumentCaptor<WebSocketEvent> captor = ArgumentCaptor.forClass(WebSocketEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(WebSocketEvent::destination)
                .containsExactly("/topic/menu", "/topic/combos");
    }

    @Test
    void notifyComboChangePublishesToCombosAndMenuTopics() {
        notificationService.notifyComboChange("availability_updated", 20L);

        ArgumentCaptor<WebSocketEvent> captor = ArgumentCaptor.forClass(WebSocketEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(WebSocketEvent::destination)
                .containsExactly("/topic/combos", "/topic/menu");
    }

    @Test
    void notifyMenuAvailabilityChangePublishesToMenuCombosAndKitchenTopics() {
        notificationService.notifyMenuAvailabilityChange();

        ArgumentCaptor<WebSocketEvent> captor = ArgumentCaptor.forClass(WebSocketEvent.class);
        verify(eventPublisher, times(3)).publishEvent(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(WebSocketEvent::destination)
                .containsExactly("/topic/menu", "/topic/combos", "/topic/kitchen");
    }

    @Test
    void notifyServiceRequestChangePublishesToServiceRequestsAndTablesTopicsIncludingAutoClosedNullIds() {
        notificationService.notifyServiceRequestChange("CREATED", 15L, 5L);
        notificationService.notifyServiceRequestChange("AUTO_CLOSED", null, null);

        ArgumentCaptor<WebSocketEvent> captor = ArgumentCaptor.forClass(WebSocketEvent.class);
        verify(eventPublisher, times(4)).publishEvent(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(WebSocketEvent::destination)
                .containsExactly(
                        "/topic/service-requests", "/topic/tables", "/topic/service-requests", "/topic/tables");
    }
}
