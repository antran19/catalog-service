package com.nexus.catalog.application.port.out;

import com.nexus.catalog.domain.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepositoryPort {
    Category save(Category category);
    Optional<Category> findById(String id);
    List<Category> findByParentId(String parentId);
    List<Category> findRoots();
    List<Category> findAll();
    void deleteById(String id);
}
