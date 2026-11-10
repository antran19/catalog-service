package com.nexus.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_prices")
public class ProductPriceJpaEntity {

    @Id
    private UUID id;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "set_by")
    private String setBy;

    protected ProductPriceJpaEntity() {
    }

    public ProductPriceJpaEntity(UUID id, UUID skuId, BigDecimal price, Instant effectiveFrom, String setBy) {
        this.id = id;
        this.skuId = skuId;
        this.price = price;
        this.effectiveFrom = effectiveFrom;
        this.setBy = setBy;
    }

    public UUID getId() { return id; }
    public UUID getSkuId() { return skuId; }
    public BigDecimal getPrice() { return price; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public String getSetBy() { return setBy; }
}
