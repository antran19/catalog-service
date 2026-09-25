package com.nexus.catalog.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Product {
    private final String id;
    private final String name;
    private final String description;
    private final String categoryId;
    private final ProductStatus status;
    private final String sellerId;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Product(String id, String name, String description, String categoryId,
                     ProductStatus status, String sellerId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.status = status;
        this.sellerId = sellerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Product create(String name, String description, String categoryId, String sellerId) {
        Instant now = Instant.now();
        return new Product(UUID.randomUUID().toString(), name, description, categoryId,
                ProductStatus.DRAFT, sellerId, now, now);
    }

    public static Product reconstitute(String id, String name, String description, String categoryId,
                                        ProductStatus status, String sellerId, Instant createdAt, Instant updatedAt) {
        return new Product(id, name, description, categoryId, status, sellerId, createdAt, updatedAt);
    }

    public Product withStatus(ProductStatus newStatus) {
        return new Product(id, name, description, categoryId, newStatus, sellerId, createdAt, Instant.now());
    }

    public Product withDetails(String newName, String newDescription) {
        return new Product(id, newName, newDescription, categoryId, status, sellerId, createdAt, Instant.now());
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategoryId() { return categoryId; }
    public ProductStatus getStatus() { return status; }
    public String getSellerId() { return sellerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
