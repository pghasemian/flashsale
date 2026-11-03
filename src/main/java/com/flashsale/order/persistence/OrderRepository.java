package com.flashsale.order.persistence;

import com.flashsale.order.domain.Order;
import com.flashsale.order.domain.OrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByReservationId(Long reservationId);

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(OrderStatus status);
}