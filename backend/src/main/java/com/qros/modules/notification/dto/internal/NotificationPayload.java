package com.qros.modules.notification.dto.internal;

public record NotificationPayload(String event, Object id, Object data) {

    public static NotificationPayload event(String event) {
        return new NotificationPayload(event, null, null);
    }

    public static NotificationPayload eventWithId(String event, Object id) {
        return new NotificationPayload(event, id, null);
    }

    public static NotificationPayload eventWithData(String event, Object data) {
        return new NotificationPayload(event, null, data);
    }

    public static NotificationPayload eventWithIdAndData(String event, Object id, Object data) {
        return new NotificationPayload(event, id, data);
    }
}
