package com.nexus.catalog.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(@NotBlank String name, String parentId) {
}
