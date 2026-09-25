package com.nexus.catalog.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(String id, String name, String description, String categoryId,
                               String status, String sellerId, List<String> imageUrls,
                               String skuId, String skuCode, BigDecimal price) {
}
