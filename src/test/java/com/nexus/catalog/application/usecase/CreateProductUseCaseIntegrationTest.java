package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.port.out.CategoryRepositoryPort;
import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.Category;
import com.nexus.catalog.infrastructure.persistence.OutboxJpaRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the Outbox pattern's transactional guarantee for real for catalog-service:
 * {@link CreateProductUseCase#create} runs against a real Postgres (via Testcontainers) with real
 * adapters wired end to end, and performs four writes in one transaction (product, images, SKU,
 * outbox row). Mirrors user-service's {@code RegisterUserUseCaseIntegrationTest}.
 *
 * <p>Deliberately NO class-level {@code @Transactional}: that annotation on a test class would open
 * its own transaction around each test and roll it back at the end regardless of what the use case
 * itself does, which would hide whether the use case's own {@code @Transactional} boundary is doing
 * anything at all.
 *
 * <p>{@link CreateProductUseCaseTest} covers the same use case with mocked ports (fast, no DB); this
 * class instead answers "if the outbox write fails mid-transaction, do the product, SKU and image
 * writes that already happened really roll back?" -- something a mock-based test cannot prove.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CreateProductUseCaseIntegrationTest {

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
    @Autowired private CategoryRepositoryPort categoryRepositoryPort;
    @Autowired private ProductRepositoryPort productRepositoryPort;
    @Autowired private SkuRepositoryPort skuRepositoryPort;
    @Autowired private ProductImagePort productImagePort;
    @Autowired private OutboxJpaRepository outboxJpaRepository;

    @Test
    void create_persistsProductSkuImagesAndAnUnpublishedOutboxRow() {
        Category category = categoryRepositoryPort.save(Category.create("Outbox-Happy Electronics", null));

        ProductResult result = createProductUseCase.create(new CreateProductCommand(
                "Outbox Laptop", "happy path", category.getId(), "seller-1",
                new BigDecimal("1299.00"), List.of("http://img/happy-1.png", "http://img/happy-2.png")));

        assertThat(productRepositoryPort.findById(result.id())).isPresent();
        assertThat(skuRepositoryPort.findByProductId(result.id()))
                .singleElement()
                .satisfies(sku -> assertThat(sku.getPrice()).isEqualByComparingTo("1299.00"));
        assertThat(productImagePort.findUrlsByProductId(result.id()))
                .containsExactly("http://img/happy-1.png", "http://img/happy-2.png");

        assertThat(outboxJpaRepository.findAll())
                .anySatisfy(row -> {
                    assertThat(row.getAggregateId()).isEqualTo(result.id());
                    assertThat(row.getEventType()).isEqualTo("ProductCreated");
                    assertThat(row.getPublishedAt()).isNull();
                    assertThat(row.getPayload()).contains("Outbox Laptop");
                });
    }

    /**
     * Separate nested Spring context (via {@code @Import}) that substitutes a throwing
     * {@link EventPublisherPort} for the real outbox-writing adapter, so the
     * product/images/SKU-save-then-throw sequence happens inside the use case's own real
     * {@code @Transactional} boundary against the real database, letting us assert on rollback
     * rather than infer it.
     */
    @Nested
    @Import(WhenOutboxWriteFails.FailingEventPublisherConfig.class)
    class WhenOutboxWriteFails {

        @Autowired private CreateProductUseCase createProductUseCase;
        @Autowired private CategoryRepositoryPort categoryRepositoryPort;
        @Autowired private JdbcTemplate jdbcTemplate;

        @Test
        void create_rollsBackProductSkuAndImageWritesWhenTheOutboxWriteThrows() {
            Category category = categoryRepositoryPort.save(Category.create("Outbox-Fail Electronics", null));
            long productsBefore = count("products");
            long skusBefore = count("skus");
            long imagesBefore = count("product_images");

            assertThatThrownBy(() -> createProductUseCase.create(new CreateProductCommand(
                    "Rolled Back Laptop", "should never persist", category.getId(), "seller-1",
                    new BigDecimal("10.00"), List.of("http://img/rollback-1.png"))))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("simulated outbox failure");

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM products WHERE name = 'Rolled Back Laptop'", Long.class))
                    .as("product save must roll back when the outbox write in the same transaction fails")
                    .isZero();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM product_images WHERE url = 'http://img/rollback-1.png'", Long.class))
                    .as("image saves must roll back too")
                    .isZero();
            assertThat(count("products")).isEqualTo(productsBefore);
            assertThat(count("skus")).as("SKU save must roll back too").isEqualTo(skusBefore);
            assertThat(count("product_images")).isEqualTo(imagesBefore);
        }

        private long count(String table) {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        }

        @TestConfiguration
        static class FailingEventPublisherConfig {
            @Bean
            @Primary
            EventPublisherPort failingEventPublisherPort() {
                return event -> {
                    throw new RuntimeException("simulated outbox failure");
                };
            }
        }
    }
}
