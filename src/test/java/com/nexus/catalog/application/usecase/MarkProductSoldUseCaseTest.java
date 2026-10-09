package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.ProductStatus;
import com.nexus.common.events.ProductStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MarkProductSoldUseCaseTest {

    private ProductRepositoryPort productRepositoryPort;
    private EventPublisherPort eventPublisherPort;
    private MarkProductSoldUseCase useCase;

    @BeforeEach
    void setUp() {
        productRepositoryPort = mock(ProductRepositoryPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new MarkProductSoldUseCase(productRepositoryPort, eventPublisherPort);
    }

    @Test
    void markSold_activeProduct_savesSoldAndPublishesEvent() {
        Product existing = Product.reconstitute("p-1", "Laptop", "desc", "cat-1",
                ProductStatus.ACTIVE, "seller-1", Instant.now(), Instant.now());
        when(productRepositoryPort.findById("p-1")).thenReturn(Optional.of(existing));
        when(productRepositoryPort.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.markSold("p-1");

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(ProductStatus.SOLD);

        ArgumentCaptor<ProductStatusChangedEvent> event = ArgumentCaptor.forClass(ProductStatusChangedEvent.class);
        verify(eventPublisherPort).publish(event.capture());
        assertThat(event.getValue().getProductId()).isEqualTo("p-1");
        assertThat(event.getValue().getOldStatus()).isEqualTo("ACTIVE");
        assertThat(event.getValue().getNewStatus()).isEqualTo("SOLD");
    }

    @Test
    void markSold_alreadySold_isANoOpWithoutEvent() {
        Product alreadySold = Product.reconstitute("p-2", "Laptop", "desc", "cat-1",
                ProductStatus.SOLD, "seller-1", Instant.now(), Instant.now());
        when(productRepositoryPort.findById("p-2")).thenReturn(Optional.of(alreadySold));

        useCase.markSold("p-2");

        verify(productRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void markSold_unknownProduct_doesNotThrow() {
        // Defensive, not an error case: a duplicate/out-of-order Kafka delivery referencing
        // a product that (for whatever reason) isn't in this service's DB must not crash
        // the listener thread -- same fail-soft convention used for other event consumers.
        when(productRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatCode(() -> useCase.markSold("missing")).doesNotThrowAnyException();

        verify(eventPublisherPort, never()).publish(any());
    }
}
