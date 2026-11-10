package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.ProductNotFoundException;
import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SellingPriceRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.Sku;
import com.nexus.common.core.FieldError;
import com.nexus.common.core.exception.ValidationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// The seller (or an admin) sets the SELLING price: what buyers pay. It can never go below the
// SKU's original price, so a price typed by mistake cannot sell the goods at a loss. Each call
// takes effect at once and is kept in the price history.
public class SetSellingPriceUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final SkuRepositoryPort skuRepositoryPort;
    private final SellingPriceRepositoryPort sellingPriceRepositoryPort;
    private final ProductImagePort productImagePort;

    public SetSellingPriceUseCase(ProductRepositoryPort productRepositoryPort,
                                   SkuRepositoryPort skuRepositoryPort,
                                   SellingPriceRepositoryPort sellingPriceRepositoryPort,
                                   ProductImagePort productImagePort) {
        this.productRepositoryPort = productRepositoryPort;
        this.skuRepositoryPort = skuRepositoryPort;
        this.sellingPriceRepositoryPort = sellingPriceRepositoryPort;
        this.productImagePort = productImagePort;
    }

    @Transactional
    public ProductResult setSellingPrice(String productId, BigDecimal price, String callerId, boolean canManageAny) {
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        ProductOwnershipPolicy.requireOwnerOrManageAny(product, callerId, canManageAny);

        Sku sku = skuRepositoryPort.findByProductId(productId).get(0);
        if (price.compareTo(sku.getOriginalPrice()) < 0) {
            throw new ValidationException(List.of(new FieldError("price",
                    "Selling price must not be below the original price")));
        }

        Sku saved = skuRepositoryPort.save(sku.withSellingPrice(price));
        sellingPriceRepositoryPort.record(saved.getId(), saved.getPrice(), callerId);

        return new ProductResult(product.getId(), product.getName(), product.getDescription(),
                product.getCategoryId(), product.getStatus().name(), product.getSellerId(),
                productImagePort.findUrlsByProductId(productId), saved.getId(), saved.getSkuCode(), saved.getPrice());
    }
}
