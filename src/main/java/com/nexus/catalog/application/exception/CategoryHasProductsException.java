package com.nexus.catalog.application.exception;

import com.nexus.common.core.exception.ConflictException;

public class CategoryHasProductsException extends ConflictException {
    public CategoryHasProductsException(String id) {
        super("CATEGORY_HAS_PRODUCTS", "Category has products and cannot be deleted: " + id);
    }
}
