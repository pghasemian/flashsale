
package com.flashsale.order.api;

import com.flashsale.dto.order.OrderResponse;
import com.flashsale.exception.ResourceNotFoundException;
import com.flashsale.order.application.OrderService;
import com.flashsale.order.domain.OrderStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private OrderResponse createOrder() {
        return new OrderResponse(
                1L,
                500L,
                OrderStatus.PENDING,
                new BigDecimal("250.00"),
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    void shouldGetMyOrdersSuccessfully() throws Exception {

        OrderResponse order = createOrder();

        when(orderService.getOrders(101L, false))
                .thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/orders")
                        .with(jwt().jwt(jwt -> jwt.subject("101"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].reservationId").value(500));
    }

    @Test
    void shouldReturn404WhenOrderBelongsToAnotherUser() throws Exception {

        when(orderService.getOrder(1L, 101L, false))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(get("/api/v1/orders/1")
                        .with(jwt().jwt(jwt -> jwt.subject("101"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Order not found"));
    }

    @Test
    void shouldReturn401WhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldGetMyOrderSuccessfully() throws Exception {

        OrderResponse order = createOrder();

        when(orderService.getOrder(1L, 101L, false))
                .thenReturn(order);

        mockMvc.perform(get("/api/v1/orders/1")
                        .with(jwt().jwt(jwt -> jwt.subject("101"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.reservationId").value(500));
    }

    @Test
    void shouldAllowAdminToGetAllOrders() throws Exception {

        OrderResponse order = createOrder();

        when(orderService.getOrders(101L, true))
                .thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/orders")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("101"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void shouldAllowAdminToGetAnyOrder() throws Exception {

        OrderResponse order = createOrder();

        when(orderService.getOrder(1L, 101L, true))
                .thenReturn(order);

        mockMvc.perform(get("/api/v1/orders/1")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("101"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }
}
