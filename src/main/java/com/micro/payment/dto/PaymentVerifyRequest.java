package com.micro.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payment verification request payload")
public record PaymentVerifyRequest(
        @NotNull(message = "Razorpay order id is required")
        @Schema(name = "razorpayOrderId", example = "order_xxxxx")
        String razorpayOrderId,

        @NotNull(message = "Razorpay payment id is required")
        @Schema(name = "razorpayPaymentId", example = "pay_xxxxx")
        String razorpayPaymentId,

        @NotNull(message = "Razorpay signature is required")
        @Schema(name = "razorpaySignature", example = "xxxxx")
        String razorpaySignature
) {
}
