package com.nexus.catalog.infrastructure.persistence;

import com.nexus.catalog.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {
    List<ProductJpaEntity> findByCategoryId(UUID categoryId);

    @Query(value = """
            SELECT * FROM products p
            WHERE (:categoryId IS NULL OR p.category_id = CAST(:categoryId AS uuid))
              AND (:status IS NULL OR p.status = :status)
              AND (:sellerId IS NULL OR p.seller_id = :sellerId)
              AND (:query IS NULL OR :query = '' OR p.search_vector @@ plainto_tsquery('english', :query))
            ORDER BY p.created_at DESC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<ProductJpaEntity> search(
            @Param("query") String query,
            @Param("categoryId") String categoryId,
            @Param("status") String status,
            @Param("sellerId") String sellerId,
            @Param("size") int size,
            @Param("offset") int offset);
}
