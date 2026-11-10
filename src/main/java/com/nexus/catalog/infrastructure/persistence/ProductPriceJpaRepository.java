package com.nexus.catalog.infrastructure.persistence;

import com.nexus.catalog.infrastructure.persistence.entity.ProductPriceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductPriceJpaRepository extends JpaRepository<ProductPriceJpaEntity, UUID> {
    List<ProductPriceJpaEntity> findBySkuIdOrderByEffectiveFromDesc(UUID skuId);
}
