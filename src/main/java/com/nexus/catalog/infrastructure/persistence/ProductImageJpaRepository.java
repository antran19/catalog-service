package com.nexus.catalog.infrastructure.persistence;

import com.nexus.catalog.infrastructure.persistence.entity.ProductImageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ProductImageJpaRepository extends JpaRepository<ProductImageJpaEntity, UUID> {
    List<ProductImageJpaEntity> findByProductIdOrderBySortOrderAsc(UUID productId);

    @Transactional
    void deleteByProductId(UUID productId);
}
