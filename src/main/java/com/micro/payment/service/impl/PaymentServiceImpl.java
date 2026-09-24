package com.micro.payment.service.impl;

import com.micro.auth.dto.response.ApiResponse;
import com.micro.auth.dto.response.PagedResponse;
import com.micro.auth.entity.User;
import com.micro.auth.enums.Role;
import com.micro.auth.exception.ApiException;
import com.micro.order.entity.Order;
import com.micro.order.enums.OrderStatus;
import com.micro.order.repository.OrderRepository;
import com.micro.payment.dto.PaymentRequest;
import com.micro.payment.dto.PaymentResponse;
import com.micro.payment.dto.PaymentVerifyRequest;
import com.micro.payment.entity.Payment;
import com.micro.payment.enums.PaymentStatus;
import com.micro.payment.repository.PaymentRepository;
import com.micro.payment.service.PaymentService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import io.lettuce.core.json.JsonObject;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Override
    @Transactional
    public ApiResponse<PaymentResponse> makePayment(Authentication authentication, PaymentRequest request) throws RazorpayException {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> ApiException.notFound("Order not found"));

        User currentUser = (User) authentication.getPrincipal();
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        if (!isAdmin && !order.getUser().getId().equals(currentUser.getId()))
            throw ApiException.badRequest("You are not allowed to pay this order");

        if (order.getStatus() != OrderStatus.CREATED)
            throw ApiException.badRequest("Payment already completed or order cancelled");

        if (paymentRepository.existsByOrderId(order.getId()))
            throw ApiException.badRequest("Payment already exists for this order");

//        String transactionId =
//                "TXN-" + UUID.randomUUID()
//                        .toString()
//                        .substring(0, 8);

        JSONObject options = new JSONObject();
        options.put(
                "amount",
                order.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue()     //RAZORPAY expects as paisa
        );
        options.put("currency", "INR");
        options.put("receipt", "ORDER_" + order.getId());

        com.razorpay.Order razorpayOrder = razorpayClient.orders.create(options);
        String razorpayOrderId = razorpayOrder.get("id");

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .paymentMethod(request.paymentMethod())
                .status(PaymentStatus.PENDING)
//                .transactionId(transactionId)
                .razorpayOrderId(razorpayOrderId)
                .build();

//        order.setStatus(OrderStatus.PAID);
//        orderRepository.save(order);
        Payment savedPayment = paymentRepository.save(payment);

        PaymentResponse response = PaymentResponse.builder()
                .id(savedPayment.getId())
                .orderId(savedPayment.getOrder().getId())
                .amount(savedPayment.getAmount())
                .paymentMethod(savedPayment.getPaymentMethod())
                .paymentStatus(savedPayment.getStatus())
//                .transactionId(savedPayment.getTransactionId())
                .razorpayOrderId(savedPayment.getRazorpayOrderId())
                .razorpayKeyId(razorpayKeyId)
                .orderStatus(savedPayment.getOrder().getStatus())
                .createdAt(savedPayment.getCreatedAt())
                .updatedAt(savedPayment.getUpdatedAt())
                .build();

        return ApiResponse.success(
                "Payment initiated successfully",
                response
        );
    }

    @Override
    public ApiResponse<PaymentResponse> verifyPayment(Authentication authentication, PaymentVerifyRequest request) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.razorpayOrderId())
                .orElseThrow(() -> ApiException.notFound("Payment not found"));

        User currentUser = (User) authentication.getPrincipal();
        boolean isAdmin = currentUser.getRole().equals(Role.ADMIN);

        if (!isAdmin && !payment.getOrder().getUser().getId().equals(currentUser.getId()))
            throw ApiException.badRequest("You are not allowed to verify this payment");

        if (payment.getOrder().getStatus() != OrderStatus.CREATED)
            throw ApiException.badRequest("Payment cannot be completed for this order");

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw ApiException.badRequest("Payment already processed");
        }

        try {
            JSONObject json = new JSONObject();
            json.put("razorpay_order_id", request.razorpayOrderId());
            json.put("razorpay_payment_id", request.razorpayPaymentId());
            json.put("razorpay_signature", request.razorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(json, razorpayKeySecret);

            if (!isValid) throw ApiException.badRequest("Invalid Razorpay payment signature");
        } catch (RazorpayException e) {
            throw ApiException.badRequest("Invalid Razorpay payment signature");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(request.razorpayPaymentId());

        Order order = payment.getOrder();
        order.setStatus(OrderStatus.PAID);
        Payment savedPayment = paymentRepository.save(payment);

        PaymentResponse response = PaymentResponse.builder()
                .id(savedPayment.getId())
                .orderId(savedPayment.getOrder().getId())
                .amount(savedPayment.getAmount())
                .paymentMethod(savedPayment.getPaymentMethod())
                .paymentStatus(savedPayment.getStatus())
                .transactionId(savedPayment.getTransactionId())
                .razorpayOrderId(savedPayment.getRazorpayOrderId())
                .orderStatus(savedPayment.getOrder().getStatus())
                .createdAt(savedPayment.getCreatedAt())
                .updatedAt(savedPayment.getUpdatedAt())
                .build();

        return ApiResponse.success(
                "Payment verified successfully",
                response
        );
    }

    @Override
    public ApiResponse<PagedResponse<PaymentResponse>> myPayments(Authentication authentication, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        User user = (User) authentication.getPrincipal();
        Page<Payment> pagedPayments = paymentRepository.findByOrderUserId(user.getId(), pageable);

        List<PaymentResponse> payments = pagedPayments.getContent().stream()
                .map(payment -> PaymentResponse.builder()
                        .id(payment.getId())
                        .orderId(payment.getOrder().getId())
                        .amount(payment.getAmount())
                        .paymentMethod(payment.getPaymentMethod())
                        .paymentStatus(payment.getStatus())
                        .transactionId(payment.getTransactionId())
                        .razorpayOrderId(payment.getRazorpayOrderId())
                        .orderStatus(payment.getOrder().getStatus())
                        .createdAt(payment.getCreatedAt())
                        .updatedAt(payment.getUpdatedAt())
                        .build()
                ).toList();

        PagedResponse response = PagedResponse.<PaymentResponse>builder()
                .content(payments)
                .page(pagedPayments.getNumber())
                .size(pagedPayments.getSize())
                .totalElements(pagedPayments.getTotalElements())
                .totalPages(pagedPayments.getTotalPages())
                .last(pagedPayments.isLast())
                .build();

        return ApiResponse.success(
                "Payments fetched successfully",
                response
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
                .razorpayOrderId(payment.getRazorpayOrderId())
                .orderStatus(payment.getOrder().getStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();

        return ApiResponse.success(
                "Payment fetched successfully",
                response
        );
    }
}
