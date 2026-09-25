package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.CategoryNotFoundException;
import com.nexus.catalog.application.port.out.CategoryRepositoryPort;
import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.Category;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.Sku;
import com.nexus.common.core.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateProductUseCaseTest {

    private ProductRepositoryPort productRepositoryPort;
    private SkuRepositoryPort skuRepositoryPort;
    private ProductImagePort productImagePort;
    private CategoryRepositoryPort categoryRepositoryPort;
    private EventPublisherPort eventPublisherPort;
    private CreateProductUseCase useCase;

    @BeforeEach
    void setUp() {
        productRepositoryPort = mock(ProductRepositoryPort.class);
        skuRepositoryPort = mock(SkuRepositoryPort.class);
        productImagePort = mock(ProductImagePort.class);
        categoryRepositoryPort = mock(CategoryRepositoryPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new CreateProductUseCase(productRepositoryPort, skuRepositoryPort, productImagePort, categoryRepositoryPort, eventPublisherPort);

        when(categoryRepositoryPort.findById("cat-id"))
                .thenReturn(Optional.of(Category.reconstitute("cat-id", "Electronics", null, Instant.now())));
        when(productRepositoryPort.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(skuRepositoryPort.save(any(Sku.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_savesProductAndDefaultSku() {
        ProductResult result = useCase.create(new CreateProductCommand(
                "Laptop X", "desc", "cat-id", "seller-id", new BigDecimal("999.99"), List.of("http://img/1.png")));

        assertThat(result.name()).isEqualTo("Laptop X");
        assertThat(result.status()).isEqualTo("DRAFT");
        assertThat(result.price()).isEqualByComparingTo("999.99");
        verify(productImagePort).saveAll(eq(result.id()), eq(List.of("http://img/1.png")));
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void create_rejectsUnknownCategory() {
        when(categoryRepositoryPort.findById("missing-cat")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.create(new CreateProductCommand(
                "Laptop X", "desc", "missing-cat", "seller-id", new BigDecimal("999.99"), List.of())))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void create_rejectsMoreThanFiveImages() {
        List<String> sixImages = List.of("1", "2", "3", "4", "5", "6");

        assertThatThrownBy(() -> useCase.create(new CreateProductCommand(
                "Laptop X", "desc", "cat-id", "seller-id", new BigDecimal("999.99"), sixImages)))
                .isInstanceOf(ValidationException.class);

        verify(productRepositoryPort, never()).save(any());
    }
}
