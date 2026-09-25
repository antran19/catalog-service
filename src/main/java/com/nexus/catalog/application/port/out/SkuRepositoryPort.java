package com.nexus.catalog.application.port.out;

import com.nexus.catalog.domain.model.Sku;

import java.util.List;

public interface SkuRepositoryPort {
    Sku save(Sku sku);
    List<Sku> findByProductId(String productId);
    void deleteByProductId(String productId);
}
