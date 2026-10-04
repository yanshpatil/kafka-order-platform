package com.yansh.platform.order.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank String userId,
        @NotEmpty @Valid List<Item> items) {

    public record Item(
            @NotBlank String productId,
            @Min(1) int quantity,
            @NotNull @DecimalMin("0.01") BigDecimal price) {
    }
}
