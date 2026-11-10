package com.nexus.catalog.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// V6: the database itself refuses values the application already forbids.
// No surrounding transaction: PostgreSQL aborts a transaction after a failed statement, so each
// insert must commit (or fail) on its own for a test to try several bad rows in a row.
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CatalogCheckConstraintsTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("catalog_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID insertCategory() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO categories (id, name) VALUES (?, 'Electronics')", id);
        return id;
    }

    private UUID insertProduct(String status) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO products (id, name, category_id, status, seller_id) VALUES (?, 'Laptop', ?, ?, 'seller-1')",
                id, insertCategory(), status);
        return id;
    }

    // The original price equals the selling price, except for a selling price of zero or below: then
    // the original price is 1, so that only the selling-price constraint (V6) is the one that fires.
    private void insertSku(UUID productId, String price) {
        jdbcTemplate.update("INSERT INTO skus (id, product_id, sku_code, price, original_price) VALUES (?, ?, ?, CAST(? AS NUMERIC), "
                        + "CASE WHEN CAST(? AS NUMERIC) > 0 THEN CAST(? AS NUMERIC) ELSE 1 END)",
                UUID.randomUUID(), productId, "SKU-" + UUID.randomUUID(), price, price, price);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "ACTIVE", "INACTIVE", "SOLD"})
    void everyProductStatusTheApplicationKnows_isAccepted(String status) {
        assertThatCode(() -> insertProduct(status)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "DELETED", "active", ""})
    void aProductStatusOutsideTheEnum_isRejected(String status) {
        assertThatThrownBy(() -> insertProduct(status))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_products_status");
    }

    @Test
    void aSkuWithAPositivePrice_isAccepted() {
        UUID productId = insertProduct("ACTIVE");
        assertThatCode(() -> insertSku(productId, "0.01")).doesNotThrowAnyException();
    }

    @Test
    void aSellingPriceBelowTheOriginalPrice_isRejected() {
        UUID productId = insertProduct("ACTIVE");
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO skus (id, product_id, sku_code, price, original_price) VALUES (?, ?, ?, 99, 100)",
                UUID.randomUUID(), productId, "SKU-" + UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_skus_selling_not_below_original");
    }

    @Test
    void aSellingPriceAboveTheOriginalPrice_isAccepted() {
        UUID productId = insertProduct("ACTIVE");
        assertThatCode(() -> jdbcTemplate.update(
                "INSERT INTO skus (id, product_id, sku_code, price, original_price) VALUES (?, ?, ?, 150, 100)",
                UUID.randomUUID(), productId, "SKU-" + UUID.randomUUID())).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1.00"})
    void aSkuPricedAtZeroOrBelow_isRejected(String price) {
        UUID productId = insertProduct("ACTIVE");
        assertThatThrownBy(() -> insertSku(productId, price))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_skus_price_positive");
    }
}
