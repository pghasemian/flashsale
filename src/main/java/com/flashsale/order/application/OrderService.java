package com.flashsale.order.application;

import com.flashsale.dto.order.OrderResponse;
import com.flashsale.exception.ResourceNotFoundException;
import com.flashsale.mapper.OrderMapper;
import com.flashsale.order.domain.Order;
import com.flashsale.order.persistence.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    public OrderResponse getMyOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found")
                );

        return OrderMapper.toResponse(order);
    }
}