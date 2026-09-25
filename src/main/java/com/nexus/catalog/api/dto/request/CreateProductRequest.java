package com.nexus.catalog.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
        @NotBlank String name,
        String description,
        @NotBlank String categoryId,
        @NotNull @DecimalMin(value = "0.01", message = "price must be greater than zero") BigDecimal price,
        List<String> imageUrls) {
}
