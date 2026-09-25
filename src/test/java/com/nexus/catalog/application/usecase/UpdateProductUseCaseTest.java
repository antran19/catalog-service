package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.ProductNotFoundException;
import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.ProductStatus;
import com.nexus.catalog.domain.model.Sku;
import com.nexus.common.core.exception.ForbiddenException;
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

class UpdateProductUseCaseTest {

    private ProductRepositoryPort productRepositoryPort;
    private SkuRepositoryPort skuRepositoryPort;
    private ProductImagePort productImagePort;
    private EventPublisherPort eventPublisherPort;
    private UpdateProductUseCase useCase;

    @BeforeEach
    void setUp() {
        productRepositoryPort = mock(ProductRepositoryPort.class);
        skuRepositoryPort = mock(SkuRepositoryPort.class);
        productImagePort = mock(ProductImagePort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new UpdateProductUseCase(productRepositoryPort, skuRepositoryPort, productImagePort, eventPublisherPort);
    }

    @Test
    void update_changesNameAndDescription() {
        Product existing = Product.reconstitute("p-id", "Old Name", "Old desc", "cat-id",
                ProductStatus.DRAFT, "seller-id", Instant.now(), Instant.now());
        when(productRepositoryPort.findById("p-id")).thenReturn(Optional.of(existing));
        when(productRepositoryPort.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productImagePort.findUrlsByProductId("p-id")).thenReturn(List.of());
        when(skuRepositoryPort.findByProductId("p-id")).thenReturn(
                List.of(Sku.reconstitute("sku-id", "p-id", "SKU-1", new BigDecimal("10.00"), null)));

        ProductResult result = useCase.update("p-id", "New Name", "New desc", "seller-id", false);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.description()).isEqualTo("New desc");
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void update_rejectsUnknownProduct() {
        when(productRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.update("missing", "New Name", "New desc", "seller-id", false))
                .isInstanceOf(ProductNotFoundException.class);
    }

    private void stubExistingProductOwnedBy(String sellerId) {
        Product existing = Product.reconstitute("p-id", "Old Name", "Old desc", "cat-id",
                ProductStatus.DRAFT, sellerId, Instant.now(), Instant.now());
        when(productRepositoryPort.findById("p-id")).thenReturn(Optional.of(existing));
        when(productRepositoryPort.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productImagePort.findUrlsByProductId("p-id")).thenReturn(List.of());
        when(skuRepositoryPort.findByProductId("p-id")).thenReturn(
                List.of(Sku.reconstitute("sku-id", "p-id", "SKU-1", new BigDecimal("10.00"), null)));
    }

    @Test
    void update_rejectsNonOwnerWithoutManageAny() {
        stubExistingProductOwnedBy("seller-id");

        assertThatThrownBy(() -> useCase.update("p-id", "Hijacked", "desc", "other-seller", false))
                .isInstanceOf(ForbiddenException.class);

        verify(productRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void update_allowsNonOwnerHoldingManageAny() {
        stubExistingProductOwnedBy("seller-id");

        ProductResult result = useCase.update("p-id", "Moderated", "desc", "admin-id", true);

        assertThat(result.name()).isEqualTo("Moderated");
        // Ownership is preserved: an admin edit must not re-assign the product to the admin.
        assertThat(result.sellerId()).isEqualTo("seller-id");
    }
}