package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.CategoryNotFoundException;
import com.nexus.catalog.application.port.out.CategoryRepositoryPort;
import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.Sku;
import com.nexus.catalog.domain.service.ProductImagePolicy;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class CreateProductUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final SkuRepositoryPort skuRepositoryPort;
    private final ProductImagePort productImagePort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public CreateProductUseCase(ProductRepositoryPort productRepositoryPort,
                                 SkuRepositoryPort skuRepositoryPort,
                                 ProductImagePort productImagePort,
                                 CategoryRepositoryPort categoryRepositoryPort,
                                 EventPublisherPort eventPublisherPort) {
        this.productRepositoryPort = productRepositoryPort;
        this.skuRepositoryPort = skuRepositoryPort;
        this.productImagePort = productImagePort;
        this.categoryRepositoryPort = categoryRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Transactional
    public ProductResult create(CreateProductCommand command) {
        categoryRepositoryPort.findById(command.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));

        ProductImagePolicy.validate(command.imageUrls().size());

        Product product = Product.create(command.name(), command.description(), command.categoryId(), command.sellerId());
        Product savedProduct = productRepositoryPort.save(product);

        productImagePort.saveAll(savedProduct.getId(), command.imageUrls());

        String skuCode = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Sku sku = Sku.create(savedProduct.getId(), skuCode, command.price(), null);
        Sku savedSku = skuRepositoryPort.save(sku);

        eventPublisherPort.publish(new com.nexus.common.events.ProductCreatedEvent(
                savedProduct.getId(), savedProduct.getName(), savedProduct.getCategoryId()));

        return new ProductResult(savedProduct.getId(), savedProduct.getName(), savedProduct.getDescription(),
                savedProduct.getCategoryId(), savedProduct.getStatus().name(), savedProduct.getSellerId(),
                command.imageUrls(), savedSku.getId(), savedSku.getSkuCode(), savedSku.getPrice());
    }
}
