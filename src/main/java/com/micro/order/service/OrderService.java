package com.micro.order.service;

import com.micro.auth.dto.response.ApiResponse;
import com.micro.auth.dto.response.PagedResponse;
import com.micro.order.dto.OrderRequest;
import com.micro.order.dto.OrderResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface OrderService {
    ApiResponse<OrderResponse> createOrder(Authentication authentication, OrderRequest request);

    ApiResponse<PagedResponse<OrderResponse>> myOrders(Authentication authentication, int page, int size, String sortBy, String sortDir);

    ApiResponse<PagedResponse<OrderResponse>> getOrders(int page, int size, String sortBy, String sortDir);

    ApiResponse<OrderResponse> getOrder(Long id);

    ApiResponse<OrderResponse> cancelOrder(Authentication authentication, Long id);
}
