package com.nexus.catalog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    void create_startsInDraftStatus() {
        Product product = Product.create("Laptop X", "A great laptop", "cat-id", "seller-id");

        assertThat(product.getId()).isNotBlank();
        assertThat(product.getName()).isEqualTo("Laptop X");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getCategoryId()).isEqualTo("cat-id");
        assertThat(product.getSellerId()).isEqualTo("seller-id");
        assertThat(product.getCreatedAt()).isEqualTo(product.getUpdatedAt());
    }

    @Test
    void withStatus_returnsUpdatedCopyWithoutMutatingOriginal() {
        Product product = Product.create("Laptop X", "desc", "cat-id", "seller-id");

        Product activated = product.withStatus(ProductStatus.ACTIVE);

        assertThat(activated.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(activated.getId()).isEqualTo(product.getId());
    }
}
