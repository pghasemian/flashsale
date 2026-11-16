
package com.flashsale.order.api;

import com.flashsale.dto.common.ApiResponse;
import com.flashsale.dto.order.OrderResponse;
import com.flashsale.order.application.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders(
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        Long userId = Long.parseLong(jwt.getSubject());
        boolean isAdmin = isAdmin(authentication);

        List<OrderResponse> orders =
                orderService.getOrders(userId, isAdmin);

        return ApiResponse.success(
                "Orders retrieved successfully", orders);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        Long userId = Long.parseLong(jwt.getSubject());
        boolean isAdmin = isAdmin(authentication);

        OrderResponse order = orderService.getOrder(id, userId, isAdmin);

        return ApiResponse.success(
                "Order retrieved successfully", order);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
