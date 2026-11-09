package com.flashsale.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductPatchRequest(

        @Size(max = 150)
        @Pattern(regexp = ".*\\S.*", message = "Name must not be blank")
        String name,

        @Size(max = 100)
        @Pattern(regexp = ".*\\S.*", message = "SKU must not be blank")
        String sku,

        @Size(max = 2000)
        String description,

        @DecimalMin("0.01")
        BigDecimal price

) {}