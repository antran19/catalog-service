package com.nexus.catalog.infrastructure.persistence;

import com.nexus.catalog.domain.model.Product;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProductRepositoryAdapter.class)
class ProductRepositoryAdapterTest {

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
    private ProductRepositoryAdapter adapter;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private String seedCategory() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO categories (id, name, created_at) VALUES (?, ?, now())", id, "Electronics");
        return id.toString();
    }

    @Test
    void saveThenFindById_roundTripsTheProduct() {
        String categoryId = seedCategory();
        Product product = Product.create("Laptop X", "A great laptop", categoryId, "seller-id");

        adapter.save(product);
        Optional<Product> found = adapter.findById(product.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Laptop X");
        assertThat(found.get().getStatus().name()).isEqualTo("DRAFT");
    }

    @Test
    void findByCategoryId_returnsProductsInThatCategory() {
        String categoryId = seedCategory();
        Product product = Product.create("Laptop X", "desc", categoryId, "seller-id");
        adapter.save(product);

        List<Product> found = adapter.findByCategoryId(categoryId);

        assertThat(found).hasSize(1);
    }

    // A malformed id is simply an id that matches nothing: it must map to "not found" (-> 404 via
    // ProductNotFoundException), not leak an IllegalArgumentException from UUID.fromString (-> 500).
    @Test
    void findById_returnsEmptyForMalformedId() {
        assertThat(adapter.findById("not-a-uuid")).isEmpty();
    }

    @Test
    void findByCategoryId_returnsEmptyForMalformedId() {
        assertThat(adapter.findByCategoryId("not-a-uuid")).isEmpty();
    }
}