package com.yansh.platform.common.events;

public record PaymentFailedEvent(String eventId, String orderId, String reason) {
}
