package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.port.out.CategoryRepositoryPort;
import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the final-review finding C1: DELETE /api/v1/products/{id} used to fail
 * with a foreign-key violation for every product created through the API, because skus and
 * product_images reference products(id) with no ON DELETE CASCADE and the use case only
 * deleted the product row. The controller test mocks the use case and so could never catch
 * this; only a real Postgres with the real Flyway schema can.
 *
 * <p>No class-level {@code @Transactional}: the product must be genuinely committed (as it is
 * in production) before the delete runs, and the delete must commit or fail on its own.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DeleteProductUseCaseIntegrationTest {

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

    @Autowired private CreateProductUseCase createProductUseCase;
    @Autowired private DeleteProductUseCase deleteProductUseCase;
    @Autowired private CategoryRepositoryPort categoryRepositoryPort;
    @Autowired private ProductRepositoryPort productRepositoryPort;
    @Autowired private SkuRepositoryPort skuRepositoryPort;
    @Autowired private ProductImagePort productImagePort;

    @Test
    void delete_removesTheProductAndItsSkuAndImages() {
        Category category = categoryRepositoryPort.save(Category.create("Delete-Test Electronics", null));
        ProductResult created = createProductUseCase.create(new CreateProductCommand(
                "Doomed Laptop", "about to be deleted", category.getId(), "seller-1",
                new BigDecimal("499.00"), List.of("http://img/a.png", "http://img/b.png")));

        // Sanity: the children really exist before the delete, so the assertions below are
        // proving removal rather than trivially passing on rows that were never written.
        assertThat(skuRepositoryPort.findByProductId(created.id())).hasSize(1);
        assertThat(productImagePort.findUrlsByProductId(created.id())).hasSize(2);

        deleteProductUseCase.delete(created.id(), "seller-1", false);

        assertThat(productRepositoryPort.findById(created.id())).isEmpty();
        assertThat(skuRepositoryPort.findByProductId(created.id())).isEmpty();
        assertThat(productImagePort.findUrlsByProductId(created.id())).isEmpty();
    }
}
