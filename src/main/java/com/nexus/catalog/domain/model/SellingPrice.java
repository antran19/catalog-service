package com.nexus.catalog.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

// One entry of a SKU's selling-price history: the price a buyer pays from effectiveFrom on.
public record SellingPrice(BigDecimal price, Instant effectiveFrom) {
}
