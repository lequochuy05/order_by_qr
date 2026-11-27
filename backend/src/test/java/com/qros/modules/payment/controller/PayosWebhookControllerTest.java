package com.qros.modules.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.qros.modules.payment.dto.internal.PaymentWebhookResult;
import com.qros.modules.payment.dto.response.WebhookResponse;
import com.qros.modules.payment.gateway.PayosGateway;
import com.qros.modules.payment.service.PaymentService;
import com.qros.shared.subscription.SubscriptionWebhookPort;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import vn.payos.model.webhooks.Webhook;

class PayosWebhookControllerTest {

    @SuppressWarnings("unchecked")
    @Test
    void routesSubscriptionOrderCodeToSubscriptionPortWithoutCallingOrderPaymentService() {
        PayosGateway payosGateway = mock(PayosGateway.class);
        PaymentService paymentService = mock(PaymentService.class);
        SubscriptionWebhookPort subscriptionPort = mock(SubscriptionWebhookPort.class);
        ObjectProvider<SubscriptionWebhookPort> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(subscriptionPort);

        Webhook webhook = new Webhook();
        PaymentWebhookResult verified =
                new PaymentWebhookResult(900_000_000_001L, BigDecimal.valueOf(2000), "pl-sub-1", "{}");
        when(payosGateway.verifyWebhook(webhook)).thenReturn(verified);
        when(subscriptionPort.tryHandleWebhook(any())).thenReturn(true);

        PayosWebhookController controller = new PayosWebhookController(payosGateway, paymentService, provider);
        ResponseEntity<WebhookResponse> response = controller.handlePayosWebhook(webhook);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(subscriptionPort).tryHandleWebhook(any());
        verifyNoInteractions(paymentService);
    }

    @SuppressWarnings("unchecked")
    @Test
    void fallsBackToOrderPaymentServiceForNonSubscriptionOrderCodes() {
        PayosGateway payosGateway = mock(PayosGateway.class);
        PaymentService paymentService = mock(PaymentService.class);
        SubscriptionWebhookPort subscriptionPort = mock(SubscriptionWebhookPort.class);
        ObjectProvider<SubscriptionWebhookPort> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(subscriptionPort);

        Webhook webhook = new Webhook();
        PaymentWebhookResult verified = new PaymentWebhookResult(100050L, BigDecimal.valueOf(85000), "pl-ord-1", "{}");
        when(payosGateway.verifyWebhook(webhook)).thenReturn(verified);
        when(subscriptionPort.tryHandleWebhook(any())).thenReturn(false);

        PayosWebhookController controller = new PayosWebhookController(payosGateway, paymentService, provider);
        ResponseEntity<WebhookResponse> response = controller.handlePayosWebhook(webhook);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(subscriptionPort).tryHandleWebhook(any());
        verify(paymentService).confirmPaymentFromWebhook(verified);
    }
}
