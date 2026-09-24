package com.micro.payment.schema;

import com.micro.auth.dto.response.ApiResponse;
import com.micro.auth.dto.response.PagedResponse;
import com.micro.payment.dto.PaymentResponse;

import java.time.LocalDateTime;
import java.util.List;

public class PaymentListResponseSchema extends ApiResponse<PagedResponse<PaymentResponse>> {
    public PaymentListResponseSchema(boolean apiStatus, String message, PagedResponse<PaymentResponse> data, Object errors, LocalDateTime timeStamp) {
        super(apiStatus, message, data, errors, timeStamp);
    }
}
