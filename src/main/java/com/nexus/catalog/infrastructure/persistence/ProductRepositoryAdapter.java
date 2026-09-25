package com.nexus.catalog.infrastructure.persistence;

import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.ProductStatus;
import com.nexus.catalog.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProductRepositoryAdapter implements ProductRepositoryPort {

    private final ProductJpaRepository jpaRepository;

    public ProductRepositoryAdapter(ProductJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = new ProductJpaEntity(
                UUID.fromString(product.getId()), product.getName(), product.getDescription(),
                UUID.fromString(product.getCategoryId()), product.getStatus().name(),
                product.getSellerId(), product.getCreatedAt(), product.getUpdatedAt());
        jpaRepository.save(entity);
        return product;
    }

    @Override
    public Optional<Product> findById(String id) {
        return UuidIds.tryParse(id).flatMap(jpaRepository::findById).map(this::toDomain);
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(UUID.fromString(id));
    }

    @Override
    public List<Product> findByCategoryId(String categoryId) {
        return UuidIds.tryParse(categoryId)
                .map(uuid -> jpaRepository.findByCategoryId(uuid).stream().map(this::toDomain).toList())
                .orElse(List.of());
    }

    @Override
    public List<Product> search(String query, String categoryId, String status, int page, int size) {
        int offset = page * size;
        return jpaRepository.search(query, categoryId, status, size, offset).stream()
                .map(this::toDomain)
                .toList();
    }

    private Product toDomain(ProductJpaEntity entity) {
        return Product.reconstitute(entity.getId().toString(), entity.getName(), entity.getDescription(),
                entity.getCategoryId().toString(), ProductStatus.valueOf(entity.getStatus()),
                entity.getSellerId(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
