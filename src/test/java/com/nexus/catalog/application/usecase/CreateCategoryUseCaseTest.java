package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.port.out.CategoryRepositoryPort;
import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.common.core.exception.ValidationException;
import com.nexus.catalog.domain.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateCategoryUseCaseTest {

    private CategoryRepositoryPort repositoryPort;
    private EventPublisherPort eventPublisherPort;
    private CreateCategoryUseCase useCase;

    @BeforeEach
    void setUp() {
        repositoryPort = mock(CategoryRepositoryPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new CreateCategoryUseCase(repositoryPort, eventPublisherPort);
        when(repositoryPort.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_savesRootCategoryWhenNoParent() {
        CategoryResult result = useCase.create(new CreateCategoryCommand("Electronics", null));

        assertThat(result.name()).isEqualTo("Electronics");
        assertThat(result.parentId()).isNull();
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void create_savesThirdLevelCategory() {
        Category root = Category.reconstitute("root-id", "Electronics", null, Instant.now());
        Category child = Category.reconstitute("child-id", "Laptops", "root-id", Instant.now());
        when(repositoryPort.findById("child-id")).thenReturn(Optional.of(child));
        when(repositoryPort.findById("root-id")).thenReturn(Optional.of(root));

        CategoryResult result = useCase.create(new CreateCategoryCommand("Gaming Laptops", "child-id"));

        assertThat(result.parentId()).isEqualTo("child-id");
    }

    @Test
    void create_rejectsFourthLevelCategory() {
        Category root = Category.reconstitute("root-id", "Electronics", null, Instant.now());
        Category child = Category.reconstitute("child-id", "Laptops", "root-id", Instant.now());
        Category grandchild = Category.reconstitute("gc-id", "Gaming Laptops", "child-id", Instant.now());
        when(repositoryPort.findById("gc-id")).thenReturn(Optional.of(grandchild));
        when(repositoryPort.findById("child-id")).thenReturn(Optional.of(child));
        when(repositoryPort.findById("root-id")).thenReturn(Optional.of(root));

        assertThatThrownBy(() -> useCase.create(new CreateCategoryCommand("Too Deep", "gc-id")))
                .isInstanceOf(ValidationException.class);

        verify(repositoryPort, never()).save(any());
    }
}
