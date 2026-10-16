package com.nexus.catalog.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
        @NotBlank String name,
        String description,
        @NotBlank String categoryId,
        @NotNull @DecimalMin(value = "0.01", message = "price must be greater than zero") BigDecimal price,
        List<String> imageUrls,
        @Min(value = 0, message = "initialStock must not be negative")
        @Max(value = 1_000_000, message = "initialStock must be at most 1000000") Integer initialStock) {
}
