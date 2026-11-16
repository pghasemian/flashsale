package com.flashsale.dto.order;

import com.flashsale.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(

        Long id,
        Long reservationId,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt

) {
}