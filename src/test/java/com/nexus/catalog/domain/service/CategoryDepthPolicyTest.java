package com.nexus.catalog.domain.service;

import com.nexus.common.core.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryDepthPolicyTest {

    @Test
    void validate_acceptsDepthUpToThree() {
        assertThatCode(() -> CategoryDepthPolicy.validate(1)).doesNotThrowAnyException();
        assertThatCode(() -> CategoryDepthPolicy.validate(2)).doesNotThrowAnyException();
        assertThatCode(() -> CategoryDepthPolicy.validate(3)).doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsDepthFour() {
        assertThatThrownBy(() -> CategoryDepthPolicy.validate(4))
                .isInstanceOf(ValidationException.class);
    }
}
