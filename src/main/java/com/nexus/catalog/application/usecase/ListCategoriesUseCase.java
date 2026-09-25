package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.port.out.CategoryRepositoryPort;

import java.util.List;

public class ListCategoriesUseCase {

    private final CategoryRepositoryPort repositoryPort;

    public ListCategoriesUseCase(CategoryRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public List<CategoryResult> listAll() {
        return repositoryPort.findAll().stream()
                .map(c -> new CategoryResult(c.getId(), c.getName(), c.getParentId()))
                .toList();
    }
}
