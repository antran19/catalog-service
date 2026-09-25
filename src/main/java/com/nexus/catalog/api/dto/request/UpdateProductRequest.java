package com.nexus.catalog.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateProductRequest(@NotBlank String name, String description) {
}
