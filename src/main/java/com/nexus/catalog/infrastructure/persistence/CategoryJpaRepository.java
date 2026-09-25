package com.nexus.catalog.infrastructure.persistence;

import com.nexus.catalog.infrastructure.persistence.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {
    List<CategoryJpaEntity> findByParentId(UUID parentId);
    List<CategoryJpaEntity> findByParentIdIsNull();
}
