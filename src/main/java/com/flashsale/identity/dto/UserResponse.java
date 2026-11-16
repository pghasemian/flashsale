
package com.flashsale.identity.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        Instant createdAt
) {
}
