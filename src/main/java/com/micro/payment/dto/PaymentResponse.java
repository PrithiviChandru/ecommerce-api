package com.micro.payment.dto;

import com.micro.order.enums.OrderStatus;
import com.micro.payment.enums.PaymentMethod;
import com.micro.payment.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String transactionId;
    private OrderStatus orderStatus;
    private String razorpayOrderId;
    private Instant createdAt;
    private Instant updatedAt;
}
