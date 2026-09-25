package com.nexus.catalog.infrastructure.persistence;

import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.Sku;
import com.nexus.catalog.infrastructure.persistence.entity.SkuJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class SkuRepositoryAdapter implements SkuRepositoryPort {

    private final SkuJpaRepository jpaRepository;

    public SkuRepositoryAdapter(SkuJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Sku save(Sku sku) {
        SkuJpaEntity entity = new SkuJpaEntity(
                UUID.fromString(sku.getId()), UUID.fromString(sku.getProductId()),
                sku.getSkuCode(), sku.getPrice(), sku.getAttributes());
        jpaRepository.save(entity);
        return sku;
    }

    @Override
    public List<Sku> findByProductId(String productId) {
        return UuidIds.tryParse(productId)
                .map(uuid -> jpaRepository.findByProductId(uuid).stream()
                        .map(e -> Sku.reconstitute(e.getId().toString(), e.getProductId().toString(),
                                e.getSkuCode(), e.getPrice(), e.getAttributes()))
                        .toList())
                .orElse(List.of());
    }

    @Override
    public void deleteByProductId(String productId) {
        jpaRepository.deleteByProductId(UUID.fromString(productId));
    }
}
