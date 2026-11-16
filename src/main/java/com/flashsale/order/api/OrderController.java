package com.flashsale.order.api;

import com.flashsale.dto.common.ApiResponse;
import com.flashsale.dto.order.OrderResponse;
import com.flashsale.order.application.OrderService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.parseLong(jwt.getSubject());

        List<OrderResponse> orders = orderService.getMyOrders(userId);

        return ApiResponse.success(
                "Orders retrieved successfully", orders);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getMyOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.parseLong(jwt.getSubject());

        OrderResponse order = orderService.getMyOrder(id, userId);

        return ApiResponse.success(
                "Order retrieved successfully", order);
    }
}