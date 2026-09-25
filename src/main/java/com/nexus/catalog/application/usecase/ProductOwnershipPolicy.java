package com.nexus.catalog.application.usecase;

import com.nexus.catalog.domain.model.Product;
import com.nexus.common.core.exception.ForbiddenException;

/**
 * Owner-or-MANAGE_ANY authorization for product mutations (update, delete, status change).
 *
 * <p>{@code @RequiresPrivilege("PRODUCT.UPDATE"/"PRODUCT.DELETE")} only proves the caller may
 * mutate <em>some</em> product; it says nothing about <em>which</em> product. Without this
 * check any seller could edit or delete any other seller's listing. Callers holding
 * {@code PRODUCT.MANAGE_ANY} (seeded for ADMIN only) bypass the ownership requirement, which is
 * the only signal available to distinguish an admin from a seller: the JWT's {@code role} claim
 * is not turned into an authority, and ADMIN/SELLER otherwise hold the same PRODUCT.* privileges.
 */
final class ProductOwnershipPolicy {

    private ProductOwnershipPolicy() {
    }

    static void requireOwnerOrManageAny(Product product, String callerId, boolean canManageAny) {
        if (!canManageAny && !product.getSellerId().equals(callerId)) {
            throw new ForbiddenException("PRODUCT_NOT_OWNED",
                    "You can only modify your own products: " + product.getId());
        }
    }
}
