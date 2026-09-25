package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.CategoryHasChildrenException;
import com.nexus.catalog.application.exception.CategoryHasProductsException;
import com.nexus.catalog.application.exception.CategoryNotFoundException;
import com.nexus.catalog.application.port.out.CategoryRepositoryPort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

public class DeleteCategoryUseCase {

    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;

    public DeleteCategoryUseCase(CategoryRepositoryPort categoryRepositoryPort,
                                  ProductRepositoryPort productRepositoryPort) {
        this.categoryRepositoryPort = categoryRepositoryPort;
        this.productRepositoryPort = productRepositoryPort;
    }

    // @Transactional: the "no children / no products" checks and the delete run in one
    // transaction, consistent with the other mutating use cases.
    @Transactional
    public void delete(String id) {
        // Spring Data's deleteById silently no-ops on a missing id, which would report success
        // (200) for a category that never existed; check first so it is a 404 like DeleteProduct.
        categoryRepositoryPort.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        if (!categoryRepositoryPort.findByParentId(id).isEmpty()) {
            throw new CategoryHasChildrenException(id);
        }
        if (!productRepositoryPort.findByCategoryId(id).isEmpty()) {
            throw new CategoryHasProductsException(id);
        }
        categoryRepositoryPort.deleteById(id);
    }
}
