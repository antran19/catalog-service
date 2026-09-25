package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.ProductNotFoundException;
import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.Sku;
import org.springframework.transaction.annotation.Transactional;

public class UpdateProductUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final SkuRepositoryPort skuRepositoryPort;
    private final ProductImagePort productImagePort;
    private final EventPublisherPort eventPublisherPort;

    public UpdateProductUseCase(ProductRepositoryPort productRepositoryPort,
                                 SkuRepositoryPort skuRepositoryPort,
                                 ProductImagePort productImagePort,
                                 EventPublisherPort eventPublisherPort) {
        this.productRepositoryPort = productRepositoryPort;
        this.skuRepositoryPort = skuRepositoryPort;
        this.productImagePort = productImagePort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Transactional
    public ProductResult update(String id, String name, String description,
                                String callerId, boolean canManageAny) {
        Product existing = productRepositoryPort.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        ProductOwnershipPolicy.requireOwnerOrManageAny(existing, callerId, canManageAny);
        Product saved = productRepositoryPort.save(existing.withDetails(name, description));

        eventPublisherPort.publish(new com.nexus.common.events.ProductUpdatedEvent(saved.getId(), saved.getName()));

        var images = productImagePort.findUrlsByProductId(id);
        Sku sku = skuRepositoryPort.findByProductId(id).get(0);

        return new ProductResult(saved.getId(), saved.getName(), saved.getDescription(), saved.getCategoryId(),
                saved.getStatus().name(), saved.getSellerId(), images, sku.getId(), sku.getSkuCode(), sku.getPrice());
    }
}
