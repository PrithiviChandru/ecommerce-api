package com.micro.payment.dto;

import com.micro.payment.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payment request payload")
public record PaymentRequest(
        @NotNull(message = "Order id is required")
        @Schema(name = "orderId", example = "1")
        Long orderId,

        @NotNull(message = "Payment method is required")
        @Schema(name = "paymentMethod", example = "CARD", allowableValues = {"CARD", "NET_BANKING", "UPI"})
        PaymentMethod paymentMethod
) {
}
