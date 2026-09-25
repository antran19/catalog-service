package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.CategoryNotFoundException;
import com.nexus.catalog.application.port.out.CategoryRepositoryPort;
import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.catalog.domain.model.Category;
import com.nexus.catalog.domain.service.CategoryDepthPolicy;
import org.springframework.transaction.annotation.Transactional;

public class CreateCategoryUseCase {

    private final CategoryRepositoryPort repositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public CreateCategoryUseCase(CategoryRepositoryPort repositoryPort, EventPublisherPort eventPublisherPort) {
        this.repositoryPort = repositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Transactional
    public CategoryResult create(CreateCategoryCommand command) {
        int depth = 1;
        if (command.parentId() != null) {
            depth = computeDepth(command.parentId()) + 1;
        }
        CategoryDepthPolicy.validate(depth);

        Category category = Category.create(command.name(), command.parentId());
        Category saved = repositoryPort.save(category);
        eventPublisherPort.publish(new com.nexus.common.events.CategoryCreatedEvent(
                saved.getId(), saved.getName(), saved.getParentId()));
        return new CategoryResult(saved.getId(), saved.getName(), saved.getParentId());
    }

    private int computeDepth(String categoryId) {
        Category current = repositoryPort.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        int depth = 1;
        while (current.getParentId() != null) {
            String parentId = current.getParentId();
            current = repositoryPort.findById(parentId)
                    .orElseThrow(() -> new CategoryNotFoundException(parentId));
            depth++;
        }
        return depth;
    }
}
