package com.nexus.catalog.application.usecase;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductCommand(String name, String description, String categoryId,
                                    String sellerId, BigDecimal price, List<String> imageUrls) {
}
