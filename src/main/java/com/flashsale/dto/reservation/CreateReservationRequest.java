package com.flashsale.dto.reservation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReservationRequest(

        @NotNull
        @Positive
        Long campaignId,

        @NotNull
        @Positive
        Long productId,

        @NotNull
        @Positive
        Integer quantity

) {
}