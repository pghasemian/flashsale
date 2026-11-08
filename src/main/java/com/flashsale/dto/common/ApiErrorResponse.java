package com.flashsale.dto.common;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        boolean success,
        String message,
        List<String> errors,
        String path,
        Instant timestamp
) {

    public static ApiErrorResponse of(
            String message,
            List<String> errors,
            String path
    ) {
        return new ApiErrorResponse(
                false,
                message,
                errors,
                path,
                Instant.now()
        );
    }
}