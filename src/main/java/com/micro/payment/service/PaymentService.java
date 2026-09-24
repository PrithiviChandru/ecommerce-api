package com.micro.payment.service;

import com.micro.auth.dto.response.ApiResponse;
import com.micro.auth.dto.response.PagedResponse;
import com.micro.payment.dto.PaymentRequest;
import com.micro.payment.dto.PaymentResponse;
import com.micro.payment.dto.PaymentVerifyRequest;
import com.razorpay.RazorpayException;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface PaymentService {
    ApiResponse<PaymentResponse> makePayment(Authentication authentication, PaymentRequest request) throws RazorpayException;

    ApiResponse<PaymentResponse> verifyPayment(Authentication authentication, PaymentVerifyRequest request);

    ApiResponse<PagedResponse<PaymentResponse>> myPayments(Authentication authentication, int page, int size, String sortBy, String sortDir);

    ApiResponse<PaymentResponse> getPayment(Authentication authentication, Long id);
}
