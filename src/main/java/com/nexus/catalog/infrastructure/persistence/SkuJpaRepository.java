package com.nexus.catalog.infrastructure.persistence;

import com.nexus.catalog.infrastructure.persistence.entity.SkuJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface SkuJpaRepository extends JpaRepository<SkuJpaEntity, UUID> {
    List<SkuJpaEntity> findByProductId(UUID productId);

    @Transactional
    void deleteByProductId(UUID productId);
}
