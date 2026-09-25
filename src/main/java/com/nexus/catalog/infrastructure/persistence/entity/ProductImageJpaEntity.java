package com.nexus.catalog.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "product_images")
public class ProductImageJpaEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ProductImageJpaEntity() {
    }

    public ProductImageJpaEntity(UUID id, UUID productId, String url, int sortOrder) {
        this.id = id;
        this.productId = productId;
        this.url = url;
        this.sortOrder = sortOrder;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getUrl() { return url; }
    public int getSortOrder() { return sortOrder; }
}
