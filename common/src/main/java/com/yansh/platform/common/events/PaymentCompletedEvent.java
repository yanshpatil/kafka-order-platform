package com.yansh.platform.common.events;

public record PaymentCompletedEvent(String eventId, String orderId, String paymentId) {
}
