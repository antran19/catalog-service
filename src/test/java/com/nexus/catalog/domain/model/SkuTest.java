package com.nexus.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkuTest {

    @Test
    void create_generatesIdAndStoresFields() {
        Sku sku = Sku.create("product-id", "LAPTOP-X-001", new BigDecimal("999.99"), null);

        assertThat(sku.getId()).isNotBlank();
        assertThat(sku.getProductId()).isEqualTo("product-id");
        assertThat(sku.getSkuCode()).isEqualTo("LAPTOP-X-001");
        assertThat(sku.getPrice()).isEqualByComparingTo("999.99");
    }

    @Test
    void create_startsSellingAtTheOriginalPrice() {
        Sku sku = Sku.create("product-id", "SKU-1", new BigDecimal("400000"), null);

        assertThat(sku.getOriginalPrice()).isEqualByComparingTo("400000");
        assertThat(sku.getPrice()).isEqualByComparingTo("400000");
    }

    @Test
    void withSellingPrice_changesOnlyTheSellingPrice() {
        Sku sku = Sku.create("product-id", "SKU-1", new BigDecimal("400000"), null)
                .withSellingPrice(new BigDecimal("550000"));

        assertThat(sku.getPrice()).isEqualByComparingTo("550000");
        assertThat(sku.getOriginalPrice()).isEqualByComparingTo("400000");
    }

    @Test
    void withSellingPrice_belowTheOriginalPrice_isRefused() {
        Sku sku = Sku.create("product-id", "SKU-1", new BigDecimal("400000"), null);

        assertThatThrownBy(() -> sku.withSellingPrice(new BigDecimal("399999")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
