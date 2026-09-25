package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.Sku;
import com.nexus.common.core.FieldError;
import com.nexus.common.core.exception.ValidationException;

import java.util.List;
import java.util.UUID;

public class SearchProductsUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final SkuRepositoryPort skuRepositoryPort;
    private final ProductImagePort productImagePort;

    public SearchProductsUseCase(ProductRepositoryPort productRepositoryPort,
                                  SkuRepositoryPort skuRepositoryPort,
                                  ProductImagePort productImagePort) {
        this.productRepositoryPort = productRepositoryPort;
        this.skuRepositoryPort = skuRepositoryPort;
        this.productImagePort = productImagePort;
    }

    public List<ProductResult> search(ProductSearchQuery query) {
        // A blank categoryId (e.g. ?categoryId=) means "no filter", not a malformed id.
        String categoryId = query.categoryId() == null || query.categoryId().isBlank() ? null : query.categoryId();
        String canonicalCategoryId = canonicalizeCategoryId(categoryId);
        List<Product> products = productRepositoryPort.search(
                query.q(), canonicalCategoryId, query.status(), query.page(), query.size());

        // N+1 per-product enrichment — acceptable at this sub-project's scale; optimize with
        // a batch/join fetch if this ever needs to handle real traffic.
        return products.stream().map(product -> {
            var images = productImagePort.findUrlsByProductId(product.getId());
            List<Sku> skus = skuRepositoryPort.findByProductId(product.getId());
            Sku sku = skus.get(0);
            return new ProductResult(product.getId(), product.getName(), product.getDescription(),
                    product.getCategoryId(), product.getStatus().name(), product.getSellerId(),
                    images, sku.getId(), sku.getSkuCode(), sku.getPrice());
        }).toList();
    }

    // The search query CASTs categoryId to uuid in native SQL, so a malformed value would fail
    // inside Postgres and surface as a 500. Reject it up front as a 400 instead. (Unlike a
    // malformed path id, which is treated as "not found", this is a filter the client supplied
    // in the wrong format, so a validation error is the more useful answer.)
    //
    // UUID.fromString is lenient about non-canonical input (e.g. "1-2-3-4-5" parses successfully
    // to 00000001-0002-0003-0004-000000000005 without throwing), so parsing alone isn't enough:
    // the RAW string is still not valid input for Postgres's CAST(... AS uuid). Returning
    // UUID#toString's canonical form here -- and passing THAT down to the query instead of the
    // original raw string -- guarantees only a canonically-formatted UUID string ever reaches
    // the native query, so a technically-valid-but-non-canonical filter value is accepted
    // (rather than rejected) and still can't trigger a 500 in Postgres.
    private static String canonicalizeCategoryId(String categoryId) {
        if (categoryId == null) {
            return null;
        }
        try {
            return UUID.fromString(categoryId).toString();
        } catch (IllegalArgumentException e) {
            throw new ValidationException(List.of(
                    new FieldError("categoryId", "categoryId must be a valid UUID")));
        }
    }
}
