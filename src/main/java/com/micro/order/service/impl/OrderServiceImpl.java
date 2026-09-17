package com.micro.order.service.impl;

import com.micro.auth.dto.response.ApiResponse;
import com.micro.auth.dto.response.PagedResponse;
import com.micro.auth.entity.User;
import com.micro.auth.enums.Role;
import com.micro.auth.exception.ApiException;
import com.micro.order.dto.OrderItemRequest;
import com.micro.order.dto.OrderItemResponse;
import com.micro.order.dto.OrderRequest;
import com.micro.order.dto.OrderResponse;
import com.micro.order.entity.Order;
import com.micro.order.entity.OrderItem;
import com.micro.order.enums.OrderStatus;
import com.micro.order.repository.OrderRepository;
import com.micro.order.service.OrderService;
import com.micro.product.entity.Product;
import com.micro.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public ApiResponse<OrderResponse> createOrder(Authentication authentication, OrderRequest request) {
        User currentUser = (User) authentication.getPrincipal();
        Order order = Order.builder().user(currentUser).build();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> ApiException.notFound("Product not found for id: " + itemRequest.productId()));

            if (product.getStock() < itemRequest.quantity())
                throw ApiException.badRequest("Insufficient stock available for product " + itemRequest.productId());

            BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            totalAmount = totalAmount.add(subTotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.quantity())
                    .price(product.getPrice())
                    .subTotal(subTotal)
                    .build();
            orderItems.add(orderItem);
            product.setStock(product.getStock() - itemRequest.quantity());
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        OrderResponse response = mapToOrderResponse(savedOrder);

        return ApiResponse.success(
                "Order created successfully",
                response
        );
    }

    @Override
    public ApiResponse<PagedResponse<OrderResponse>> myOrders(Authentication authentication, int page, int size, String sortBy, String sortDir) {
        User currentUser = (User) authentication.getPrincipal();

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Order> pagedOrders = orderRepository.findByUserId(currentUser.getId(), pageable);

        List<OrderResponse> orders = pagedOrders.stream()
                .map(this::mapToOrderResponse)
                .toList();

        PagedResponse response = PagedResponse.<OrderResponse>builder()
                .content(orders)
                .page(pagedOrders.getNumber())
                .size(pagedOrders.getSize())
                .totalElements(pagedOrders.getTotalElements())
                .totalPages(pagedOrders.getTotalPages())
                .last(pagedOrders.isLast())
                .build();

        return ApiResponse.success(
                "Order fetched successfully",
                response
        );
    }

    @Override
    public ApiResponse<PagedResponse<OrderResponse>> getOrders(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Order> pagedOrders = orderRepository.findAll(pageable);

        List<OrderResponse> orders = pagedOrders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toUnmodifiableList());

        PagedResponse<OrderResponse> response = PagedResponse.<OrderResponse>builder()
                .content(orders)
                .page(pagedOrders.getNumber())
                .size(pagedOrders.getSize())
                .totalElements(pagedOrders.getTotalElements())
                .totalPages(pagedOrders.getTotalPages())
                .last(pagedOrders.isLast())
                .build();

        return ApiResponse.success(
                "Order fetched successfully",
                response
        );
    }

    @Override
    public ApiResponse<OrderResponse> getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Order not found"));

        OrderResponse response = mapToOrderResponse(order);
        return ApiResponse.success(
                "Order fetched successfully",
                response
        );
    }

    @Override
    @Transactional
    public ApiResponse<OrderResponse> cancelOrder(Authentication authentication, Long id) {
        User currentUser = (User) authentication.getPrincipal();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Order not found"));

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isAdmin && !order.getUser().getId().equals(currentUser.getId()))
            throw ApiException.badRequest("You are not allowed to cancel this order");

        if (OrderStatus.CREATED != order.getStatus())
            throw ApiException.badRequest("Only created orders can be cancelled");

        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        OrderResponse response = mapToOrderResponse(order);
        return ApiResponse.success(
                "Order cancelled successfully",
                response
        );
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userName(order.getUser().getFirstName())
                .items(order.getOrderItems().stream()
                        .map(this::mapToOrderItemResponse)
                        .collect(Collectors.toUnmodifiableList())
                )
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .productId(orderItem.getProduct().getId())
                .productName(orderItem.getProduct().getName())
                .price(orderItem.getPrice())
                .quantity(orderItem.getQuantity())
                .subTotal(orderItem.getSubTotal())
                .build();
    }
}
