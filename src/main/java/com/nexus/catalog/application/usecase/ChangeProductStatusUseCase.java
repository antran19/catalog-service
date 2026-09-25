package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.ProductNotFoundException;
import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.ProductStatus;
import com.nexus.catalog.domain.model.Sku;
import com.nexus.common.core.FieldError;
import com.nexus.common.core.exception.ValidationException;
import com.nexus.common.events.ProductStatusChangedEvent;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Moves a product between DRAFT / ACTIVE / INACTIVE. Without this, every product stayed DRAFT
 * forever and the public /discover endpoint (ACTIVE only) could never return anything.
 */
public class ChangeProductStatusUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final SkuRepositoryPort skuRepositoryPort;
    private final ProductImagePort productImagePort;
    private final EventPublisherPort eventPublisherPort;

    public ChangeProductStatusUseCase(ProductRepositoryPort productRepositoryPort,
                                       SkuRepositoryPort skuRepositoryPort,
                                       ProductImagePort productImagePort,
                                       EventPublisherPort eventPublisherPort) {
        this.productRepositoryPort = productRepositoryPort;
        this.skuRepositoryPort = skuRepositoryPort;
        this.productImagePort = productImagePort;
        this.eventPublisherPort = eventPublisherPort;
    }

    // The status save and the outbox write share this one transaction (same pattern as
    // Create/UpdateProductUseCase): either both commit or neither does.
    @Transactional
    public ProductResult changeStatus(String id, String newStatus, String callerId, boolean canManageAny) {
        Product existing = productRepositoryPort.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        ProductOwnershipPolicy.requireOwnerOrManageAny(existing, callerId, canManageAny);
        ProductStatus target = parseStatus(newStatus);

        Product result = existing;
        // Re-setting the current status is an idempotent no-op: nothing changed, so there is
        // nothing to save and no ProductStatusChanged event for downstream consumers.
        if (existing.getStatus() != target) {
            result = productRepositoryPort.save(existing.withStatus(target));
            eventPublisherPort.publish(new ProductStatusChangedEvent(
                    result.getId(), existing.getStatus().name(), target.name()));
        }

        var images = productImagePort.findUrlsByProductId(id);
        Sku sku = skuRepositoryPort.findByProductId(id).get(0);
        return new ProductResult(result.getId(), result.getName(), result.getDescription(), result.getCategoryId(),
                result.getStatus().name(), result.getSellerId(), images, sku.getId(), sku.getSkuCode(), sku.getPrice());
    }

    private static ProductStatus parseStatus(String raw) {
        try {
            return ProductStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException(List.of(new FieldError("status",
                    "Status must be one of " + Arrays.toString(ProductStatus.values()))));
        }
    }
}
