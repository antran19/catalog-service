package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.ProductNotFoundException;
import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.Product;
import org.springframework.transaction.annotation.Transactional;

public class DeleteProductUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final SkuRepositoryPort skuRepositoryPort;
    private final ProductImagePort productImagePort;

    public DeleteProductUseCase(ProductRepositoryPort productRepositoryPort,
                                 SkuRepositoryPort skuRepositoryPort,
                                 ProductImagePort productImagePort) {
        this.productRepositoryPort = productRepositoryPort;
        this.skuRepositoryPort = skuRepositoryPort;
        this.productImagePort = productImagePort;
    }

    // @Transactional: the child deletes (SKUs, images) and the product delete must commit or
    // roll back together. Without it, a failure after the child deletes would leave a product
    // with no SKU, which GetProductUseCase/SearchProductsUseCase's skus.get(0) cannot handle.
    @Transactional
    public void delete(String id, String callerId, boolean canManageAny) {
        Product existing = productRepositoryPort.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        ProductOwnershipPolicy.requireOwnerOrManageAny(existing, callerId, canManageAny);
        // The SRS's ALLOW_PRODUCT_DELETE_WITH_ORDER check is intentionally NOT implemented
        // here: it requires knowing whether the product has existing orders, and Commerce
        // Service (which owns Order data) does not exist yet in this codebase. Deletion is
        // unconditionally allowed for now; wiring the real check is Commerce Service's job.

        // skus and product_images reference products(id) with no ON DELETE CASCADE, so the
        // children must go first or the product delete fails with a foreign-key violation.
        skuRepositoryPort.deleteByProductId(id);
        productImagePort.deleteByProductId(id);
        productRepositoryPort.deleteById(id);
    }
}
