package com.nexus.catalog.application.exception;

import com.nexus.common.core.exception.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {
    public CategoryNotFoundException(String id) {
        super("CATEGORY_NOT_FOUND", "Category not found: " + id);
    }
}
