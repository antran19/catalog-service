package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.domain.model.ProductStatus;
import com.nexus.common.events.ProductStatusChangedEvent;
import org.springframework.transaction.annotation.Transactional;

// The auction's winner defaulted (AuctionPaymentTimeout): MarkProductSoldUseCase had marked the
// product SOLD when the auction was won, but no sale happened and fulfillment has released the
// held unit. Puts it back on sale so the seller can sell it or auction it again. System-triggered
// like MarkProductSold (no ownership check), idempotent, and only from SOLD: a status the seller
// set since is left alone. The ProductStatusChanged event updates commerce's and auction's copies.
public class RelistProductUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public RelistProductUseCase(ProductRepositoryPort productRepositoryPort, EventPublisherPort eventPublisherPort) {
        this.productRepositoryPort = productRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Transactional
    public void relist(String productId) {
        // Null for a timeout published before the event carried the product.
        if (productId == null) {
            return;
        }
        productRepositoryPort.findById(productId).ifPresent(product -> {
            if (product.getStatus() != ProductStatus.SOLD) {
                return;
            }
            productRepositoryPort.save(product.withStatus(ProductStatus.ACTIVE));
            eventPublisherPort.publish(new ProductStatusChangedEvent(productId, "SOLD", "ACTIVE"));
        });
    }
}
