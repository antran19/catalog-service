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
import com.nexus.common.core.exception.ValidationException;
import com.nexus.common.events.ProductStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChangeProductStatusUseCaseTest {

    private ProductRepositoryPort productRepositoryPort;
    private SkuRepositoryPort skuRepositoryPort;
    private ProductImagePort productImagePort;
    private EventPublisherPort eventPublisherPort;
    private ChangeProductStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        productRepositoryPort = mock(ProductRepositoryPort.class);
        skuRepositoryPort = mock(SkuRepositoryPort.class);
        productImagePort = mock(ProductImagePort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new ChangeProductStatusUseCase(productRepositoryPort, skuRepositoryPort, productImagePort,
                eventPublisherPort);
    }

    private void stubExistingDraftProductOwnedBy(String sellerId) {
        Product existing = Product.reconstitute("p-id", "Laptop", "desc", "cat-id",
                ProductStatus.DRAFT, sellerId, Instant.now(), Instant.now());
        when(productRepositoryPort.findById("p-id")).thenReturn(Optional.of(existing));
        when(productRepositoryPort.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productImagePort.findUrlsByProductId("p-id")).thenReturn(List.of());
        when(skuRepositoryPort.findByProductId("p-id")).thenReturn(
                List.of(Sku.reconstitute("sku-id", "p-id", "SKU-1", new BigDecimal("10.00"), null)));
    }

    @Test
    void changeStatus_ownerCanActivateOwnProduct_savesAndPublishesEvent() {
        stubExistingDraftProductOwnedBy("seller-id");

        ProductResult result = useCase.changeStatus("p-id", "ACTIVE", "seller-id", false);

        assertThat(result.status()).isEqualTo("ACTIVE");
        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(ProductStatus.ACTIVE);

        ArgumentCaptor<ProductStatusChangedEvent> event = ArgumentCaptor.forClass(ProductStatusChangedEvent.class);
        verify(eventPublisherPort).publish(event.capture());
        assertThat(event.getValue().getProductId()).isEqualTo("p-id");
        assertThat(event.getValue().getOldStatus()).isEqualTo("DRAFT");
        assertThat(event.getValue().getNewStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void changeStatus_acceptsLowercaseStatus() {
        stubExistingDraftProductOwnedBy("seller-id");

        ProductResult result = useCase.changeStatus("p-id", "active", "seller-id", false);

        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    @Test
    void changeStatus_rejectsNonOwnerWithoutManageAny() {
        stubExistingDraftProductOwnedBy("seller-id");

        assertThatThrownBy(() -> useCase.changeStatus("p-id", "ACTIVE", "other-seller", false))
                .isInstanceOf(ForbiddenException.class);

        verify(productRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void changeStatus_allowsNonOwnerHoldingManageAny() {
        stubExistingDraftProductOwnedBy("seller-id");

        ProductResult result = useCase.changeStatus("p-id", "INACTIVE", "admin-id", true);

        assertThat(result.status()).isEqualTo("INACTIVE");
        assertThat(result.sellerId()).isEqualTo("seller-id");
        verify(eventPublisherPort).publish(any(ProductStatusChangedEvent.class));
    }

    @Test
    void changeStatus_rejectsUnknownStatusValue() {
        stubExistingDraftProductOwnedBy("seller-id");

        assertThatThrownBy(() -> useCase.changeStatus("p-id", "PUBLISHED", "seller-id", false))
                .isInstanceOf(ValidationException.class);

        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void changeStatus_rejectsUnknownProduct() {
        when(productRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.changeStatus("missing", "ACTIVE", "seller-id", true))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void changeStatus_toSameStatusIsANoOpWithoutEvent() {
        stubExistingDraftProductOwnedBy("seller-id");

        ProductResult result = useCase.changeStatus("p-id", "DRAFT", "seller-id", false);

        assertThat(result.status()).isEqualTo("DRAFT");
        verify(productRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }
}
