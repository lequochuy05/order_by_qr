package com.qros.modules.payment.dto.response;

public record WebhookResponse(boolean success, String message) {

    public static WebhookResponse ok() {
        return new WebhookResponse(true, null);
    }

    public static WebhookResponse failure(String message) {
        return new WebhookResponse(false, message);
    }
}
