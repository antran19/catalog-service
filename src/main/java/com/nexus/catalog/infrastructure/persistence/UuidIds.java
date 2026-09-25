package com.nexus.catalog.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

/**
 * Lenient parsing for ids that arrive from outside (path variables, query params, request bodies).
 * Every table keys on UUID, so a string that is not a UUID cannot match any row: lookups treat it
 * as "not found" instead of letting UUID.fromString's IllegalArgumentException escape as a 500.
 */
final class UuidIds {

    private UuidIds() {
    }

    static Optional<UUID> tryParse(String id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
