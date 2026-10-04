package com.yansh.platform.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(
        String eventId,
        String orderId,
        String userId,
        List<OrderItem> items,
        BigDecimal totalAmount,
        Instant createdAt) {
}
