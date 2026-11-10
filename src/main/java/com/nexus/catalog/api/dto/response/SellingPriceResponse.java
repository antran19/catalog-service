package com.nexus.catalog.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record SellingPriceResponse(BigDecimal price, Instant effectiveFrom) {
}
