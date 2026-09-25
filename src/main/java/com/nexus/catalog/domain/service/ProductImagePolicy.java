package com.nexus.catalog.domain.service;

import com.nexus.common.core.FieldError;
import com.nexus.common.core.exception.ValidationException;

import java.util.List;

public final class ProductImagePolicy {

    public static final int MAX_IMAGES = 5;

    private ProductImagePolicy() {
    }

    public static void validate(int imageCount) {
        if (imageCount > MAX_IMAGES) {
            throw new ValidationException(List.of(
                    new FieldError("images", "A product cannot have more than " + MAX_IMAGES + " images")));
        }
    }
}
