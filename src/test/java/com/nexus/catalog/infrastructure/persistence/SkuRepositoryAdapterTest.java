package com.nexus.catalog.infrastructure.persistence;

import com.nexus.catalog.domain.model.Sku;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SkuRepositoryAdapter.class)
class SkuRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("catalog_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SkuRepositoryAdapter adapter;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private String seedProduct() {
        UUID categoryId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO categories (id, name, created_at) VALUES (?, ?, now())", categoryId, "Electronics");
        UUID productId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO products (id, name, description, category_id, status, seller_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, now(), now())",
                productId, "Laptop X", "desc", categoryId, "DRAFT", "seller-id");
        return productId.toString();
    }

    @Test
    void saveThenFindByProductId_roundTripsTheSku() {
        String productId = seedProduct();
        Sku sku = Sku.create(productId, "SKU-ABC123", new BigDecimal("999.99"), null);

        adapter.save(sku);
        List<Sku> found = adapter.findByProductId(productId);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getSkuCode()).isEqualTo("SKU-ABC123");
        assertThat(found.get(0).getPrice()).isEqualByComparingTo("999.99");
        assertThat(found.get(0).getProductId()).isEqualTo(productId);
    }

    @Test
    void findByProductId_returnsEmptyForMalformedId() {
        org.assertj.core.api.Assertions.assertThat(adapter.findByProductId("not-a-uuid")).isEmpty();
    }
}