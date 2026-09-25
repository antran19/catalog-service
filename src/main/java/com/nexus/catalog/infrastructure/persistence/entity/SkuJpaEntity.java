package com.nexus.catalog.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "skus")
public class SkuJpaEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "sku_code", nullable = false, unique = true)
    private String skuCode;

    @Column(nullable = false)
    private BigDecimal price;

    private String attributes;

    protected SkuJpaEntity() {
    }

    public SkuJpaEntity(UUID id, UUID productId, String skuCode, BigDecimal price, String attributes) {
        this.id = id;
        this.productId = productId;
        this.skuCode = skuCode;
        this.price = price;
        this.attributes = attributes;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getSkuCode() { return skuCode; }
    public BigDecimal getPrice() { return price; }
    public String getAttributes() { return attributes; }
}
