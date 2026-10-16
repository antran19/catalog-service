package com.nexus.catalog.application.usecase;

import java.math.BigDecimal;
import java.util.List;

// initialStock is how many units the seller has on hand when listing the product; it travels
// in the ProductCreated event so fulfillment-service can open the inventory record.
public record CreateProductCommand(String name, String description, String categoryId,
                                    String sellerId, BigDecimal price, List<String> imageUrls,
                                    int initialStock) {

    public CreateProductCommand(String name, String description, String categoryId,
                                String sellerId, BigDecimal price, List<String> imageUrls) {
        this(name, description, categoryId, sellerId, price, imageUrls, 0);
    }
}
