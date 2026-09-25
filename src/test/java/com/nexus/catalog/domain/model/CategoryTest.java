package com.nexus.catalog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    @Test
    void create_generatesIdAndStoresFields() {
        Category category = Category.create("Electronics", null);

        assertThat(category.getId()).isNotBlank();
        assertThat(category.getName()).isEqualTo("Electronics");
        assertThat(category.getParentId()).isNull();
        assertThat(category.getCreatedAt()).isNotNull();
    }

    @Test
    void create_acceptsAParentId() {
        Category category = Category.create("Laptops", "parent-id-123");

        assertThat(category.getParentId()).isEqualTo("parent-id-123");
    }
}
