package com.nexus.catalog.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Sku {
    private final String id;
    private final String productId;
    private final String skuCode;
    private final BigDecimal price;
    private final String attributes;

    private Sku(String id, String productId, String skuCode, BigDecimal price, String attributes) {
        this.id = id;
        this.productId = productId;
        this.skuCode = skuCode;
        this.price = price;
        this.attributes = attributes;
    }

    public static Sku create(String productId, String skuCode, BigDecimal price, String attributes) {
        return new Sku(UUID.randomUUID().toString(), productId, skuCode, price, attributes);
    }

    public static Sku reconstitute(String id, String productId, String skuCode, BigDecimal price, String attributes) {
        return new Sku(id, productId, skuCode, price, attributes);
    }

    public String getId() { return id; }
    public String getProductId() { return productId; }
    public String getSkuCode() { return skuCode; }
    public BigDecimal getPrice() { return price; }
    public String getAttributes() { return attributes; }
}
