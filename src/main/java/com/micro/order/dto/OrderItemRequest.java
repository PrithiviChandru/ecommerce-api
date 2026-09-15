package com.micro.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull(message = "Product id is required")
        @Schema(name = "productId", example = "1")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Schema(name = "quantity", example = "1")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {
}
