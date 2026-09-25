package com.nexus.catalog.application.port.out;

import com.nexus.catalog.domain.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {
    Product save(Product product);
    Optional<Product> findById(String id);
    void deleteById(String id);
    List<Product> findByCategoryId(String categoryId);
    List<Product> search(String query, String categoryId, String status, int page, int size);
}
