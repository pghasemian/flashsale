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

    public List<OrderResponse> getOrders(Long userId, boolean isAdmin) {

        List<Order> orders = isAdmin
                ? orderRepository.findAll()
                : orderRepository.findByUserId(userId);

        return orders.stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    public OrderResponse getOrder(
            Long orderId,
            Long userId,
            boolean isAdmin
    ) {
        Order order = isAdmin
                ? orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"))
                : orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        return OrderMapper.toResponse(order);
    }
}