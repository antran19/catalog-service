package com.nexus.catalog.application.usecase;

/**
 * Search parameters for the public (unauthenticated) product search/discover endpoints.
 *
 * <p>Paging is clamped rather than rejected: a negative page becomes 0 and size is forced into
 * {@code 1..MAX_SIZE}. Without this, size=100000 ran the per-product enrichment ~100k times for a
 * single anonymous request, and a negative page/size produced an invalid SQL OFFSET/LIMIT (500).
 * Page is also capped so that {@code page * size} (the SQL OFFSET, an int) can never overflow.
 */
public record ProductSearchQuery(String q, String categoryId, String status, int page, int size) {

    public static final int MAX_SIZE = 100;
    static final int MAX_PAGE = Integer.MAX_VALUE / MAX_SIZE;

    public ProductSearchQuery {
        page = Math.min(Math.max(page, 0), MAX_PAGE);
        size = Math.min(Math.max(size, 1), MAX_SIZE);
    }
}
