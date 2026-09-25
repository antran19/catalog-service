package com.nexus.catalog.application.usecase;

import java.math.BigDecimal;
import java.util.List;

public record ProductResult(String id, String name, String description, String categoryId,
                             String status, String sellerId, List<String> imageUrls,
                             String skuId, String skuCode, BigDecimal price) {
}
