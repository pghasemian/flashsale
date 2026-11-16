
package com.flashsale.identity.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {}
