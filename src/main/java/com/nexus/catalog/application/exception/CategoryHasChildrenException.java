package com.nexus.catalog.application.exception;

import com.nexus.common.core.exception.ConflictException;

public class CategoryHasChildrenException extends ConflictException {
    public CategoryHasChildrenException(String id) {
        super("CATEGORY_HAS_CHILDREN", "Category has child categories and cannot be deleted: " + id);
    }
}
