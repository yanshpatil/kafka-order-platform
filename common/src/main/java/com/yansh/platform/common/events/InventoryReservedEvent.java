package com.yansh.platform.common.events;

import java.math.BigDecimal;

public record InventoryReservedEvent(String eventId, String orderId, String userId, BigDecimal totalAmount) {
}
