package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.ProductStatus;
import com.nexus.common.events.ProductStatusChangedEvent;
import org.springframework.transaction.annotation.Transactional;

// Triggered by AuctionWon (see AuctionEventsListener), not by a human request, so unlike
// ChangeProductStatusUseCase there is no caller/ownership check here -- the auction already
// decided this outcome.
public class MarkProductSoldUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public MarkProductSoldUseCase(ProductRepositoryPort productRepositoryPort, EventPublisherPort eventPublisherPort) {
        this.productRepositoryPort = productRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Transactional
    public void markSold(String productId) {
        productRepositoryPort.findById(productId).ifPresent(product -> {
            if (product.getStatus() == ProductStatus.SOLD) {
                return;
            }
            ProductStatus oldStatus = product.getStatus();
            productRepositoryPort.save(product.withStatus(ProductStatus.SOLD));
            eventPublisherPort.publish(new ProductStatusChangedEvent(productId, oldStatus.name(), "SOLD"));
        });
    }
}
