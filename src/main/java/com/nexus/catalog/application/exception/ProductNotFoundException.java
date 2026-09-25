package com.nexus.catalog.application.exception;

import com.nexus.common.core.exception.NotFoundException;

public class ProductNotFoundException extends NotFoundException {
    public ProductNotFoundException(String id) {
        super("PRODUCT_NOT_FOUND", "Product not found: " + id);
    }
}
