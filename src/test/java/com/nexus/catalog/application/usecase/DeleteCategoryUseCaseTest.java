package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.CategoryHasChildrenException;
import com.nexus.catalog.application.exception.CategoryHasProductsException;
import com.nexus.catalog.application.exception.CategoryNotFoundException;
import com.nexus.catalog.application.port.out.CategoryRepositoryPort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.domain.model.Category;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeleteCategoryUseCaseTest {

    private CategoryRepositoryPort categoryRepositoryPort;
    private ProductRepositoryPort productRepositoryPort;
    private DeleteCategoryUseCase useCase;

    @BeforeEach
    void setUp() {
        categoryRepositoryPort = mock(CategoryRepositoryPort.class);
        productRepositoryPort = mock(ProductRepositoryPort.class);
        useCase = new DeleteCategoryUseCase(categoryRepositoryPort, productRepositoryPort);
    }

    @Test
    void delete_rejectsWhenCategoryHasChildren() {
        stubExisting("root-id");
        Category child = Category.reconstitute("child-id", "Laptops", "root-id", Instant.now());
        when(categoryRepositoryPort.findByParentId("root-id")).thenReturn(List.of(child));

        assertThatThrownBy(() -> useCase.delete("root-id"))
                .isInstanceOf(CategoryHasChildrenException.class);
    }

    @Test
    void delete_rejectsWhenCategoryHasProducts() {
        stubExisting("cat-id");
        when(categoryRepositoryPort.findByParentId("cat-id")).thenReturn(List.of());
        Product product = Product.reconstitute("p-id", "Laptop", "desc", "cat-id",
                ProductStatus.DRAFT, "seller-id", Instant.now(), Instant.now());
        when(productRepositoryPort.findByCategoryId("cat-id")).thenReturn(List.of(product));

        assertThatThrownBy(() -> useCase.delete("cat-id"))
                .isInstanceOf(CategoryHasProductsException.class);
    }

    private void stubExisting(String id) {
        when(categoryRepositoryPort.findById(id))
                .thenReturn(Optional.of(Category.reconstitute(id, "Electronics", null, Instant.now())));
    }

    @Test
    void delete_rejectsUnknownCategoryWith404InsteadOfSilentlySucceeding() {
        when(categoryRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.delete("missing"))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(categoryRepositoryPort, never()).deleteById(any());
    }

    @Test
    void delete_deletesAnExistingLeafCategoryWithNoProducts() {
        stubExisting("cat-id");
        when(categoryRepositoryPort.findByParentId("cat-id")).thenReturn(List.of());
        when(productRepositoryPort.findByCategoryId("cat-id")).thenReturn(List.of());

        useCase.delete("cat-id");

        verify(categoryRepositoryPort).deleteById("cat-id");
    }
}