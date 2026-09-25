package com.nexus.catalog.infrastructure.persistence;

import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.infrastructure.persistence.entity.ProductImageJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ProductImageRepositoryAdapter implements ProductImagePort {

    private final ProductImageJpaRepository jpaRepository;

    public ProductImageRepositoryAdapter(ProductImageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void saveAll(String productId, List<String> urls) {
        UUID productUuid = UUID.fromString(productId);
        for (int i = 0; i < urls.size(); i++) {
            jpaRepository.save(new ProductImageJpaEntity(UUID.randomUUID(), productUuid, urls.get(i), i));
        }
    }

    @Override
    public List<String> findUrlsByProductId(String productId) {
        return UuidIds.tryParse(productId)
                .map(uuid -> jpaRepository.findByProductIdOrderBySortOrderAsc(uuid).stream()
                        .map(ProductImageJpaEntity::getUrl)
                        .toList())
                .orElse(List.of());
    }

    @Override
    public void deleteByProductId(String productId) {
        jpaRepository.deleteByProductId(UUID.fromString(productId));
    }
}
