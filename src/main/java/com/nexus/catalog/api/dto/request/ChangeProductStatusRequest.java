package com.nexus.catalog.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeProductStatusRequest(@NotBlank String status) {
}
