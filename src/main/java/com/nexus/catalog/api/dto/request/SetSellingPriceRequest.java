package com.nexus.catalog.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SetSellingPriceRequest(
        @NotNull @DecimalMin(value = "0.01", message = "price must be greater than zero") BigDecimal price) {
}
