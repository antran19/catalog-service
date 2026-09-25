package com.nexus.catalog.domain.service;

import com.nexus.common.core.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductImagePolicyTest {

    @Test
    void validate_acceptsUpToFiveImages() {
        assertThatCode(() -> ProductImagePolicy.validate(5)).doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsSixImages() {
        assertThatThrownBy(() -> ProductImagePolicy.validate(6))
                .isInstanceOf(ValidationException.class);
    }
}
