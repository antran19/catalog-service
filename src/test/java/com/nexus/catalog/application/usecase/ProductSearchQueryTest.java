package com.nexus.catalog.application.usecase;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSearchQueryTest {

    @Test
    void keepsInRangeValuesUnchanged() {
        ProductSearchQuery q = new ProductSearchQuery("x", null, null, 3, 20);

        assertThat(q.page()).isEqualTo(3);
        assertThat(q.size()).isEqualTo(20);
    }

    @Test
    void clampsNegativePageToZero() {
        assertThat(new ProductSearchQuery(null, null, null, -1, 20).page()).isZero();
    }

    @Test
    void clampsSizeIntoOneToMax() {
        assertThat(new ProductSearchQuery(null, null, null, 0, 0).size()).isEqualTo(1);
        assertThat(new ProductSearchQuery(null, null, null, 0, -10).size()).isEqualTo(1);
        assertThat(new ProductSearchQuery(null, null, null, 0, 100_000).size())
                .isEqualTo(ProductSearchQuery.MAX_SIZE);
    }

    @Test
    void clampsHugePageSoTheOffsetCannotOverflowInt() {
        ProductSearchQuery q = new ProductSearchQuery(null, null, null, Integer.MAX_VALUE, 100);

        // page * size is used as the SQL OFFSET (an int); it must never overflow to a negative.
        assertThat((long) q.page() * q.size()).isLessThanOrEqualTo(Integer.MAX_VALUE);
    }
}
