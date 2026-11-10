package com.nexus.catalog.infrastructure.persistence;

import com.nexus.catalog.application.port.out.SellingPriceRepositoryPort;
import com.nexus.catalog.domain.model.SellingPrice;
import com.nexus.catalog.infrastructure.persistence.entity.ProductPriceJpaEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class SellingPriceRepositoryAdapter implements SellingPriceRepositoryPort {

    private final ProductPriceJpaRepository jpaRepository;

    public SellingPriceRepositoryAdapter(ProductPriceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void record(String skuId, BigDecimal price, String setBy) {
        jpaRepository.save(new ProductPriceJpaEntity(
                UUID.randomUUID(), UUID.fromString(skuId), price, Instant.now(), setBy));
    }

    @Override
    public List<SellingPrice> findBySkuId(String skuId) {
        return UuidIds.tryParse(skuId)
                .map(uuid -> jpaRepository.findBySkuIdOrderByEffectiveFromDesc(uuid).stream()
                        .map(e -> new SellingPrice(e.getPrice(), e.getEffectiveFrom()))
                        .toList())
                .orElse(List.of());
    }
}
