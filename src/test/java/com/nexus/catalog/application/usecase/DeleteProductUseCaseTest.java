package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.ProductNotFoundException;
import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.ProductStatus;
import com.nexus.common.core.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeleteProductUseCaseTest {

    private ProductRepositoryPort productRepositoryPort;
    private SkuRepositoryPort skuRepositoryPort;
    private ProductImagePort productImagePort;
    private DeleteProductUseCase useCase;

    @BeforeEach
    void setUp() {
        productRepositoryPort = mock(ProductRepositoryPort.class);
        skuRepositoryPort = mock(SkuRepositoryPort.class);
        productImagePort = mock(ProductImagePort.class);
        useCase = new DeleteProductUseCase(productRepositoryPort, skuRepositoryPort, productImagePort);
    }

    private void stubExistingProductOwnedBy(String sellerId) {
        Product existing = Product.reconstitute("p-id", "Laptop", "desc", "cat-id",
                ProductStatus.DRAFT, sellerId, Instant.now(), Instant.now());
        when(productRepositoryPort.findById("p-id")).thenReturn(Optional.of(existing));
    }

    @Test
    void delete_ownerCanDeleteOwnProduct_childrenDeletedBeforeProduct() {
        stubExistingProductOwnedBy("seller-id");

        useCase.delete("p-id", "seller-id", false);

        InOrder inOrder = inOrder(skuRepositoryPort, productImagePort, productRepositoryPort);
        inOrder.verify(skuRepositoryPort).deleteByProductId("p-id");
        inOrder.verify(productImagePort).deleteByProductId("p-id");
        inOrder.verify(productRepositoryPort).deleteById("p-id");
    }

    @Test
    void delete_rejectsNonOwnerWithoutManageAny() {
        stubExistingProductOwnedBy("seller-id");

        assertThatThrownBy(() -> useCase.delete("p-id", "other-seller", false))
                .isInstanceOf(ForbiddenException.class);

        verify(skuRepositoryPort, never()).deleteByProductId(any());
        verify(productImagePort, never()).deleteByProductId(any());
        verify(productRepositoryPort, never()).deleteById(any());
    }

    @Test
    void delete_allowsNonOwnerHoldingManageAny() {
        stubExistingProductOwnedBy("seller-id");

        useCase.delete("p-id", "admin-id", true);

        verify(productRepositoryPort).deleteById("p-id");
    }

    @Test
    void delete_rejectsUnknownProduct() {
        when(productRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.delete("missing", "seller-id", true))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepositoryPort, never()).deleteById(any());
    }
}
