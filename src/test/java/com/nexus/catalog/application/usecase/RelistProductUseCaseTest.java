package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.ProductStatus;
import com.nexus.common.events.ProductStatusChangedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class RelistProductUseCaseTest {

    private final ProductRepositoryPort productRepositoryPort = mock(ProductRepositoryPort.class);
    private final EventPublisherPort eventPublisherPort = mock(EventPublisherPort.class);
    private final RelistProductUseCase useCase = new RelistProductUseCase(productRepositoryPort, eventPublisherPort);

    private static Product product(String id, ProductStatus status) {
        return Product.reconstitute(id, "Watch", "desc", "cat-1", status, "seller-1", Instant.now(), Instant.now());
    }

    @Test
    void relist_aSoldProduct_putsItBackOnSaleAndTellsTheOtherServices() {
        when(productRepositoryPort.findById("p-1")).thenReturn(Optional.of(product("p-1", ProductStatus.SOLD)));
        when(productRepositoryPort.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.relist("p-1");

        verify(productRepositoryPort).save(argThat(p -> p.getStatus() == ProductStatus.ACTIVE));
        verify(eventPublisherPort).publish(argThat(e -> e instanceof ProductStatusChangedEvent evt
                && evt.getProductId().equals("p-1") && "SOLD".equals(evt.getOldStatus())
                && "ACTIVE".equals(evt.getNewStatus())));
    }

    @Test
    void relist_onlyTouchesASoldProduct() {
        // The seller may already have changed it (e.g. made it INACTIVE); never override that.
        when(productRepositoryPort.findById("p-2")).thenReturn(Optional.of(product("p-2", ProductStatus.INACTIVE)));
        when(productRepositoryPort.findById("unknown")).thenReturn(Optional.empty());

        useCase.relist("p-2");
        useCase.relist("unknown");
        useCase.relist(null);

        verify(productRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }
}
