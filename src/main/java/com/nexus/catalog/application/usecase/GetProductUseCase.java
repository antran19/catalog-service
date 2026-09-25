package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.ProductNotFoundException;
import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.Sku;

public class GetProductUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final SkuRepositoryPort skuRepositoryPort;
    private final ProductImagePort productImagePort;

    public GetProductUseCase(ProductRepositoryPort productRepositoryPort,
                              SkuRepositoryPort skuRepositoryPort,
                              ProductImagePort productImagePort) {
        this.productRepositoryPort = productRepositoryPort;
        this.skuRepositoryPort = skuRepositoryPort;
        this.productImagePort = productImagePort;
    }

    public ProductResult get(String id) {
        Product product = productRepositoryPort.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        var images = productImagePort.findUrlsByProductId(id);
        Sku sku = skuRepositoryPort.findByProductId(id).get(0);

        return new ProductResult(product.getId(), product.getName(), product.getDescription(), product.getCategoryId(),
                product.getStatus().name(), product.getSellerId(), images, sku.getId(), sku.getSkuCode(), sku.getPrice());
    }
}
