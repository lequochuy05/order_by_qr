package com.qros.modules.payment.controller;

import com.qros.modules.payment.dto.internal.PaymentWebhookResult;
import com.qros.modules.payment.dto.response.WebhookResponse;
import com.qros.modules.payment.gateway.PayosGateway;
import com.qros.modules.payment.service.PaymentService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.subscription.SubscriptionWebhookPayload;
import com.qros.shared.subscription.SubscriptionWebhookPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.webhooks.Webhook;

@Slf4j
@RestController
@RequestMapping(ApiRoutes.WEBHOOKS)
public class PayosWebhookController {

    private final PayosGateway payosGateway;
    private final PaymentService paymentService;
    private final ObjectProvider<SubscriptionWebhookPort> subscriptionWebhookPortProvider;

    public PayosWebhookController(PayosGateway payosGateway, PaymentService paymentService) {
        this(payosGateway, paymentService, null);
    }

    @Autowired
    public PayosWebhookController(
            PayosGateway payosGateway,
            PaymentService paymentService,
            ObjectProvider<SubscriptionWebhookPort> subscriptionWebhookPortProvider) {
        this.payosGateway = payosGateway;
        this.paymentService = paymentService;
        this.subscriptionWebhookPortProvider = subscriptionWebhookPortProvider;
    }

    @PostMapping("/payos")
    public ResponseEntity<WebhookResponse> handlePayosWebhook(@RequestBody Webhook webhook) {
        try {
            PaymentWebhookResult result = payosGateway.verifyWebhook(webhook);
            if (result != null && tryHandleSubscriptionWebhook(result)) {
                return ResponseEntity.ok(WebhookResponse.ok());
            }
            paymentService.confirmPaymentFromWebhook(result);
            return ResponseEntity.ok(WebhookResponse.ok());
        } catch (com.qros.shared.exception.BusinessException e) {
            log.error("[PayOS Webhook] Business error processing webhook payload", e);
            return ResponseEntity.badRequest().body(WebhookResponse.failure("Webhook payload rejected"));
        } catch (Exception e) {
            log.error("[PayOS Webhook] System error processing webhook payload", e);
            return ResponseEntity.internalServerError().body(WebhookResponse.failure("Internal processing error"));
        }
    }

    private boolean tryHandleSubscriptionWebhook(PaymentWebhookResult result) {
        if (subscriptionWebhookPortProvider == null) {
            return false;
        }
        SubscriptionWebhookPort port = subscriptionWebhookPortProvider.getIfAvailable();
        if (port == null) {
            return false;
        }
        return port.tryHandleWebhook(new SubscriptionWebhookPayload(
                result.gatewayOrderCode(), result.amount(), result.externalReference(), result.providerPayload()));
    }
}
