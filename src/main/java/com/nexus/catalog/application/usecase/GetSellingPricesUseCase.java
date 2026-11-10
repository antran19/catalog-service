package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.ProductNotFoundException;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SellingPriceRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.SellingPrice;

import java.util.List;

// Public: the selling-price history of a product, newest first. The original price is not part of it.
public class GetSellingPricesUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final SkuRepositoryPort skuRepositoryPort;
    private final SellingPriceRepositoryPort sellingPriceRepositoryPort;

    public GetSellingPricesUseCase(ProductRepositoryPort productRepositoryPort,
                                    SkuRepositoryPort skuRepositoryPort,
                                    SellingPriceRepositoryPort sellingPriceRepositoryPort) {
        this.productRepositoryPort = productRepositoryPort;
        this.skuRepositoryPort = skuRepositoryPort;
        this.sellingPriceRepositoryPort = sellingPriceRepositoryPort;
    }

    public List<SellingPrice> get(String productId) {
        productRepositoryPort.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        return sellingPriceRepositoryPort.findBySkuId(skuRepositoryPort.findByProductId(productId).get(0).getId());
    }
}
