package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.CategoryNotFoundException;
import com.nexus.catalog.application.port.out.CategoryRepositoryPort;
import com.nexus.catalog.domain.model.Category;

public class GetCategoryUseCase {

    private final CategoryRepositoryPort repositoryPort;

    public GetCategoryUseCase(CategoryRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public CategoryResult get(String id) {
        Category category = repositoryPort.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        return new CategoryResult(category.getId(), category.getName(), category.getParentId());
    }
}
