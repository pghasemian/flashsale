
package com.flashsale.order.application;

import com.flashsale.dto.order.OrderResponse;
import com.flashsale.exception.ResourceNotFoundException;
import com.flashsale.order.domain.Order;
import com.flashsale.order.domain.OrderStatus;
import com.flashsale.order.persistence.OrderRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldGetOrderWhenUserOwnsIt() {

        Order order = Order.create(
                101L,
                500L,
                new BigDecimal("250.00")
        );

        when(orderRepository.findByIdAndUserId(1L, 101L))
                .thenReturn(Optional.of(order));

        var response = orderService.getOrder(1L, 101L, false);

        assertEquals(500L, response.reservationId());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(new BigDecimal("250.00"), response.totalAmount());

        verify(orderRepository).findByIdAndUserId(1L, 101L);
        verify(orderRepository, never()).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenOrderBelongsToAnotherUser() {

        when(orderRepository.findByIdAndUserId(1L, 101L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getOrder(1L, 101L, false)
        );

        verify(orderRepository).findByIdAndUserId(1L, 101L);
    }

    @Test
    void shouldGetAllOrdersOfUser() {

        Order order1 = Order.create(
                101L, 500L, new BigDecimal("250.00")
        );

        Order order2 = Order.create(
                101L, 501L, new BigDecimal("350.00")
        );

        when(orderRepository.findByUserId(101L))
                .thenReturn(List.of(order1, order2));

        var response = orderService.getOrders(101L, false);

        assertEquals(2, response.size());
        assertEquals(500L, response.get(0).reservationId());
        assertEquals(501L, response.get(1).reservationId());

        verify(orderRepository).findByUserId(101L);
        verify(orderRepository, never()).findAll();
    }

    @Test
    void shouldThrowExceptionWhenOrderDoesNotExist() {

        when(orderRepository.findByIdAndUserId(999L, 101L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getOrder(999L, 101L, false)
        );

        verify(orderRepository).findByIdAndUserId(999L, 101L);
    }

    @Test
    void shouldAllowAdminToGetAllOrders() {

        Order order1 = Order.create(
                101L, 500L, new BigDecimal("250.00")
        );

        Order order2 = Order.create(
                102L, 501L, new BigDecimal("350.00")
        );

        when(orderRepository.findAll())
                .thenReturn(List.of(order1, order2));

        List<OrderResponse> response = orderService.getOrders(101L, true);

        assertEquals(2, response.size());
        assertEquals(500L, response.get(0).reservationId());
        assertEquals(501L, response.get(1).reservationId());

        verify(orderRepository).findAll();
        verify(orderRepository, never()).findByUserId(anyLong());
    }

    @Test
    void shouldAllowAdminToGetAnyOrder() {

        Order order = Order.create(
                102L,
                500L,
                new BigDecimal("250.00")
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        var response = orderService.getOrder(1L, 101L, true);

        assertEquals(500L, response.reservationId());

        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).findByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void shouldThrowExceptionWhenAdminRequestsNonExistingOrder() {

        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getOrder(999L, 101L, true)
        );

        verify(orderRepository).findById(999L);
    }
}
