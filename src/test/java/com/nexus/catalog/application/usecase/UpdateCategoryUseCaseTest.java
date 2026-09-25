package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.port.out.CategoryRepositoryPort;
import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.catalog.domain.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateCategoryUseCaseTest {

    private CategoryRepositoryPort repositoryPort;
    private EventPublisherPort eventPublisherPort;
    private UpdateCategoryUseCase useCase;

    @BeforeEach
    void setUp() {
        repositoryPort = mock(CategoryRepositoryPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new UpdateCategoryUseCase(repositoryPort, eventPublisherPort);
        when(repositoryPort.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void update_changesNameAndPublishesEvent() {
        Category existing = Category.reconstitute("cat-id", "Old Name", null, Instant.now());
        when(repositoryPort.findById("cat-id")).thenReturn(Optional.of(existing));

        CategoryResult result = useCase.update("cat-id", "New Name");

        assertThat(result.name()).isEqualTo("New Name");
        verify(eventPublisherPort).publish(any());
    }
}
