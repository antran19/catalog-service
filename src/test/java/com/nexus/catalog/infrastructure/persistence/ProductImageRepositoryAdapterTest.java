package com.nexus.catalog.infrastructure.persistence;

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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProductImageRepositoryAdapter.class)
class ProductImageRepositoryAdapterTest {

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
    private ProductImageRepositoryAdapter adapter;

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
    void saveAllThenFindUrlsByProductId_preservesSaveOrder() {
        String productId = seedProduct();
        List<String> urls = List.of("http://img/1.png", "http://img/2.png", "http://img/3.png");

        adapter.saveAll(productId, urls);
        List<String> found = adapter.findUrlsByProductId(productId);

        assertThat(found).containsExactlyElementsOf(urls);
    }

    @Test
    void findUrlsByProductId_returnsEmptyForMalformedId() {
        org.assertj.core.api.Assertions.assertThat(adapter.findUrlsByProductId("not-a-uuid")).isEmpty();
    }
}