package com.nexus.catalog.application.port.out;

import com.nexus.catalog.domain.model.SellingPrice;

import java.math.BigDecimal;
import java.util.List;

public interface SellingPriceRepositoryPort {
    void record(String skuId, BigDecimal price, String setBy);

    // Newest first.
    List<SellingPrice> findBySkuId(String skuId);
}
