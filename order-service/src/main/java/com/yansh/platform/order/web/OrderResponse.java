package com.yansh.platform.order.web;

import com.yansh.platform.order.domain.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String userId,
        String status,
        String failureReason,
        BigDecimal totalAmount,
        List<Line> items,
        Instant createdAt,
        Instant updatedAt) {

    public record Line(String productId, int quantity, BigDecimal price) {
    }

    public static OrderResponse from(Order o) {
        return new OrderResponse(
                o.getId(),
                o.getUserId(),
                o.getStatus().name(),
                o.getFailureReason(),
                o.getTotalAmount(),
                o.getItems().stream().map(l -> new Line(l.getProductId(), l.getQuantity(), l.getPrice())).toList(),
                o.getCreatedAt(),
                o.getUpdatedAt());
    }
}
