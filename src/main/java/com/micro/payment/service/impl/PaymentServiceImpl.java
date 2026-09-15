package com.micro.payment.service.impl;

import com.micro.auth.dto.response.ApiResponse;
import com.micro.auth.entity.User;
import com.micro.auth.exception.ApiException;
import com.micro.order.entity.Order;
import com.micro.order.enums.OrderStatus;
import com.micro.order.repository.OrderRepository;
import com.micro.payment.dto.PaymentRequest;
import com.micro.payment.dto.PaymentResponse;
import com.micro.payment.entity.Payment;
import com.micro.payment.enums.PaymentStatus;
import com.micro.payment.repository.PaymentRepository;
import com.micro.payment.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public ApiResponse<PaymentResponse> makePayment(Authentication authentication, PaymentRequest request) {
        // validate order
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> ApiException.notFound("Order not found"));

        // check ownership (USER can pay only own order)
        User currentUser = (User) authentication.getPrincipal();
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        if (!order.getUser().getId().equals(currentUser.getId()))
            throw ApiException.badRequest("You are not allowed to pay this order");

        // only CREATED orders can be paid
        if (order.getStatus() != OrderStatus.CREATED)
            throw ApiException.badRequest("Payment already completed or order cancelled");

        // prevent duplicate payment
        if (paymentRepository.existsByOrderId(order.getId()))
            throw ApiException.badRequest("Payment already exists for this order");

        String transactionId =
                "TXN-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        Payment payment = Payment.builder()
                .order(order)
//                .amount(order.getTotalAmount())
                .paymentMethod(request.paymentMethod())
                .status(PaymentStatus.SUCCESS)
                .transactionId(transactionId)
                .build();

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        Payment savedPayment = paymentRepository.save(payment);

        PaymentResponse response = PaymentResponse.builder()
                .id(savedPayment.getId())
                .orderId(order.getId())
                .amount(savedPayment.getAmount())
                .paymentMethod(savedPayment.getPaymentMethod())
                .paymentStatus(savedPayment.getStatus())
                .transactionId(savedPayment.getTransactionId())
                .orderStatus(order.getStatus())
                .createdAt(savedPayment.getCreatedAt())
                .build();

        return ApiResponse.success(
                "Payment completed successfully",
                response
        );
    }

    @Override
    public ApiResponse<List<PaymentResponse>> myPayments(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Payment> payments = paymentRepository.findByOrderUserId(user.getId());

        List<PaymentResponse> responses = payments.stream()
                .map(payment -> PaymentResponse.builder()
                        .id(payment.getId())
                        .orderId(payment.getOrder().getId())
                        .amount(payment.getAmount())
                        .paymentMethod(payment.getPaymentMethod())
                        .paymentStatus(payment.getStatus())
                        .transactionId(payment.getTransactionId())
                        .orderStatus(payment.getOrder().getStatus())
                        .createdAt(payment.getCreatedAt())
                        .build()
                ).toList();

        return ApiResponse.success(
                "Payments fetched successfully",
                responses
        );
    }

    @Override
    public ApiResponse<PaymentResponse> getPayment(Authentication authentication, Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Payment not found"));

        User user = (User) authentication.getPrincipal();
        boolean isAdmin = user.getRole().name().equals("ADMIN");

        if (!isAdmin && !payment.getOrder().getUser().getId().equals(user.getId()))
            throw ApiException.badRequest("You are not allowed to view this payment");

        PaymentResponse response = PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .orderStatus(payment.getOrder().getStatus())
                .createdAt(payment.getCreatedAt())
                .build();

        return ApiResponse.success(
                "Payment fetched successfully",
                response
        );
    }
}
