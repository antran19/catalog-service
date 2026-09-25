package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.port.out.CategoryRepositoryPort;
import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// @Transactional: each @Test method runs in its own transaction that is rolled back
// afterward, so the shared Testcontainers Postgres instance stays isolated between the
// four test methods in this class (they otherwise reuse the same SKU codes seeded in
// @BeforeEach and would collide on the unique sku_code constraint).
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class SearchProductsUseCaseTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("catalog_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired private CategoryRepositoryPort categoryRepositoryPort;
    @Autowired private ProductRepositoryPort productRepositoryPort;
    @Autowired private SkuRepositoryPort skuRepositoryPort;
    @Autowired private ProductImagePort productImagePort;
    @Autowired private SearchProductsUseCase useCase;

    private String categoryId;

    @BeforeEach
    void seed() {
        var category = com.nexus.catalog.domain.model.Category.create("Electronics", null);
        categoryRepositoryPort.save(category);
        categoryId = category.getId();

        var gamingLaptop = com.nexus.catalog.domain.model.Product.create(
                "Gaming Laptop Pro", "High-end gaming laptop with RGB", categoryId, "seller-1");
        productRepositoryPort.save(gamingLaptop);
        productImagePort.saveAll(gamingLaptop.getId(), List.of());
        skuRepositoryPort.save(com.nexus.catalog.domain.model.Sku.create(
                gamingLaptop.getId(), "SKU-GAME1", new BigDecimal("1999.99"), null));

        var officeChair = com.nexus.catalog.domain.model.Product.create(
                "Office Chair", "Ergonomic office chair", categoryId, "seller-1");
        productRepositoryPort.save(officeChair);
        productImagePort.saveAll(officeChair.getId(), List.of());
        skuRepositoryPort.save(com.nexus.catalog.domain.model.Sku.create(
                officeChair.getId(), "SKU-CHAIR1", new BigDecimal("199.99"), null));
    }

    @Test
    void search_withMatchingQuery_returnsOnlyMatchingProducts() {
        List<ProductResult> results = useCase.search(new ProductSearchQuery("gaming", null, null, 0, 20));

        assertThat(results).extracting(ProductResult::name).containsExactly("Gaming Laptop Pro");
    }

    @Test
    void search_withBlankQuery_returnsAllProducts() {
        List<ProductResult> results = useCase.search(new ProductSearchQuery("", null, null, 0, 20));

        assertThat(results).hasSize(2);
    }

    @Test
    void search_withNullQuery_returnsAllProducts() {
        List<ProductResult> results = useCase.search(new ProductSearchQuery(null, null, null, 0, 20));

        assertThat(results).hasSize(2);
    }

    @Test
    void search_filtersyByCategoryId() {
        List<ProductResult> results = useCase.search(new ProductSearchQuery(null, categoryId, null, 0, 20));

        assertThat(results).hasSize(2);
    }

    @Test
    void search_rejectsMalformedCategoryIdAs400ValidationError() {
        // Previously reached the native query's CAST(:categoryId AS uuid) and failed in Postgres (500).
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> useCase.search(new ProductSearchQuery(null, "not-a-uuid", null, 0, 20)))
                .isInstanceOf(com.nexus.common.core.exception.ValidationException.class);
    }

    @Test
    void search_withNonCanonicalButParseableUuid_isCanonicalizedInsteadOf500() {
        // UUID.fromString is lenient: "1-2-3-4-5" parses successfully (to
        // 00000001-0002-0003-0004-000000000005) without throwing, but the RAW string is not
        // valid input for Postgres's CAST(:categoryId AS uuid). If the raw string were passed
        // through unchanged, this would fail in Postgres and surface as a 500. Canonicalizing
        // via UUID#toString before it reaches the native query avoids that, and since this
        // canonical id matches no seeded product, the filter legitimately returns no results.
        List<ProductResult> results = useCase.search(new ProductSearchQuery(null, "1-2-3-4-5", null, 0, 20));

        assertThat(results).isEmpty();
    }

    @Test
    void search_withNegativePageAndOversizedSize_isClampedInsteadOfErroring() {
        // Previously a negative page produced a negative SQL OFFSET (Postgres error -> 500).
        List<ProductResult> results = useCase.search(new ProductSearchQuery(null, null, null, -5, 100_000));

        assertThat(results).hasSize(2);
    }
}