package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.CategoryNotFoundException;
import com.nexus.catalog.application.port.out.CategoryRepositoryPort;
import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.catalog.domain.model.Category;
import org.springframework.transaction.annotation.Transactional;

public class UpdateCategoryUseCase {

    private final CategoryRepositoryPort repositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public UpdateCategoryUseCase(CategoryRepositoryPort repositoryPort, EventPublisherPort eventPublisherPort) {
        this.repositoryPort = repositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Transactional
    public CategoryResult update(String id, String newName) {
        Category existing = repositoryPort.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        Category updated = Category.reconstitute(existing.getId(), newName, existing.getParentId(), existing.getCreatedAt());
        Category saved = repositoryPort.save(updated);
        eventPublisherPort.publish(new com.nexus.common.events.CategoryUpdatedEvent(saved.getId(), saved.getName()));
        return new CategoryResult(saved.getId(), saved.getName(), saved.getParentId());
    }
}
