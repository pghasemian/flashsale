package com.flashsale.dto.campaign;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CampaignUpdateRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull
        @Future
        Instant startTime,

        @NotNull
        @Future
        Instant endTime

) {}