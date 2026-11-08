package com.flashsale.dto.product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(

        Long id,
        String name,
        String description,
        String sku,
        BigDecimal price,
        boolean active,
        Instant createdAt,
        Instant updatedAt

) {
}