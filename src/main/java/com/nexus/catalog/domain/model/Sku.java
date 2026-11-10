package com.nexus.catalog.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Sku {
    private final String id;
    private final String productId;
    private final String skuCode;
    // The SELLING price: what a buyer pays. Never below originalPrice.
    private final BigDecimal price;
    // The cost price of the goods. Never shown to buyers.
    private final BigDecimal originalPrice;
    private final String attributes;

    private Sku(String id, String productId, String skuCode, BigDecimal price, BigDecimal originalPrice,
                String attributes) {
        this.id = id;
        this.productId = productId;
        this.skuCode = skuCode;
        this.price = price;
        this.originalPrice = originalPrice;
        this.attributes = attributes;
    }

    // A new SKU starts selling at its original price; the seller raises the selling price later.
    public static Sku create(String productId, String skuCode, BigDecimal originalPrice, String attributes) {
        return new Sku(UUID.randomUUID().toString(), productId, skuCode, originalPrice, originalPrice, attributes);
    }

    // Convenience for callers that only know one price: it is both original and selling.
    public static Sku reconstitute(String id, String productId, String skuCode, BigDecimal price, String attributes) {
        return new Sku(id, productId, skuCode, price, price, attributes);
    }

    public static Sku reconstitute(String id, String productId, String skuCode, BigDecimal price,
                                   BigDecimal originalPrice, String attributes) {
        return new Sku(id, productId, skuCode, price, originalPrice, attributes);
    }

    public Sku withSellingPrice(BigDecimal sellingPrice) {
        if (sellingPrice.compareTo(originalPrice) < 0) {
            throw new IllegalArgumentException("Selling price " + sellingPrice
                    + " is below the original price " + originalPrice);
        }
        return new Sku(id, productId, skuCode, sellingPrice, originalPrice, attributes);
    }

    public String getId() { return id; }
    public String getProductId() { return productId; }
    public String getSkuCode() { return skuCode; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public String getAttributes() { return attributes; }
}
