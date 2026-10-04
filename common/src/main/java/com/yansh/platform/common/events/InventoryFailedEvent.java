package com.yansh.platform.common.events;

public record InventoryFailedEvent(String eventId, String orderId, String reason) {
}
