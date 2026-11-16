package com.flashsale.order.application;

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

        var response = orderService.getMyOrder(1L, 101L);

        assertEquals(500L, response.reservationId());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(new BigDecimal("250.00"), response.totalAmount());

        verify(orderRepository).findByIdAndUserId(1L, 101L);
    }

    @Test
    void shouldThrowExceptionWhenOrderBelongsToAnotherUser() {

        when(orderRepository.findByIdAndUserId(1L, 101L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getMyOrder(1L, 101L)
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

        var response = orderService.getMyOrders(101L);

        assertEquals(2, response.size());
        assertEquals(500L, response.get(0).reservationId());
        assertEquals(501L, response.get(1).reservationId());

        verify(orderRepository).findByUserId(101L);
    }

    @Test
    void shouldThrowExceptionWhenOrderDoesNotExist() {

        when(orderRepository.findByIdAndUserId(999L, 101L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getMyOrder(999L, 101L)
        );

        verify(orderRepository).findByIdAndUserId(999L, 101L);
    }
}