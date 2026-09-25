package com.nexus.catalog.infrastructure.persistence;

import com.nexus.catalog.application.port.out.CategoryRepositoryPort;
import com.nexus.catalog.domain.model.Category;
import com.nexus.catalog.infrastructure.persistence.entity.CategoryJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CategoryRepositoryAdapter implements CategoryRepositoryPort {

    private final CategoryJpaRepository jpaRepository;

    public CategoryRepositoryAdapter(CategoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Category save(Category category) {
        UUID parentId = category.getParentId() == null ? null : UUID.fromString(category.getParentId());
        CategoryJpaEntity entity = new CategoryJpaEntity(
                UUID.fromString(category.getId()), category.getName(), parentId, category.getCreatedAt());
        jpaRepository.save(entity);
        return category;
    }

    @Override
    public Optional<Category> findById(String id) {
        return UuidIds.tryParse(id).flatMap(jpaRepository::findById).map(this::toDomain);
    }

    @Override
    public List<Category> findByParentId(String parentId) {
        return UuidIds.tryParse(parentId)
                .map(uuid -> jpaRepository.findByParentId(uuid).stream().map(this::toDomain).toList())
                .orElse(List.of());
    }

    @Override
    public List<Category> findRoots() {
        return jpaRepository.findByParentIdIsNull().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Category> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(UUID.fromString(id));
    }

    private Category toDomain(CategoryJpaEntity entity) {
        String parentId = entity.getParentId() == null ? null : entity.getParentId().toString();
        return Category.reconstitute(entity.getId().toString(), entity.getName(), parentId, entity.getCreatedAt());
    }
}
