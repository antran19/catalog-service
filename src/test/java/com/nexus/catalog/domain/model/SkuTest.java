package com.nexus.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SkuTest {

    @Test
    void create_generatesIdAndStoresFields() {
        Sku sku = Sku.create("product-id", "LAPTOP-X-001", new BigDecimal("999.99"), null);

        assertThat(sku.getId()).isNotBlank();
        assertThat(sku.getProductId()).isEqualTo("product-id");
        assertThat(sku.getSkuCode()).isEqualTo("LAPTOP-X-001");
        assertThat(sku.getPrice()).isEqualByComparingTo("999.99");
    }
}
