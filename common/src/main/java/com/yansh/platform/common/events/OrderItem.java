package com.yansh.platform.common.events;

import java.math.BigDecimal;

public record OrderItem(String productId, int quantity, BigDecimal price) {
}
