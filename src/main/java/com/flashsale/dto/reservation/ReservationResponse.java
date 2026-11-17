package com.flashsale.dto.reservation;

import com.flashsale.reservation.domain.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ReservationResponse(

        Long id,
        Long campaignId,
        Long productId,
        int quantity,
        ReservationStatus status,
        BigDecimal unitPrice,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt

) {
}