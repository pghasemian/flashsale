package com.flashsale.dto.campaign;

import java.time.Instant;
import java.util.UUID;

public record CampaignResponse(

        Long id,

        String name,

        String description,

        Instant startTime,

        Instant endTime,

        String status,

        Instant createdAt,

        Instant updatedAt

) {
}