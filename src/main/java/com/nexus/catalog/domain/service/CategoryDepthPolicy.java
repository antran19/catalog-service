package com.nexus.catalog.domain.service;

import com.nexus.common.core.FieldError;
import com.nexus.common.core.exception.ValidationException;

import java.util.List;

public final class CategoryDepthPolicy {

    public static final int MAX_DEPTH = 3;

    private CategoryDepthPolicy() {
    }

    public static void validate(int depth) {
        if (depth > MAX_DEPTH) {
            throw new ValidationException(List.of(
                    new FieldError("parentId", "Category hierarchy cannot exceed " + MAX_DEPTH + " levels")));
        }
    }
}
