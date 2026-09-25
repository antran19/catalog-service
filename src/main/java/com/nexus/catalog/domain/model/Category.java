package com.nexus.catalog.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Category {
    private final String id;
    private final String name;
    private final String parentId;
    private final Instant createdAt;

    private Category(String id, String name, String parentId, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.createdAt = createdAt;
    }

    public static Category create(String name, String parentId) {
        return new Category(UUID.randomUUID().toString(), name, parentId, Instant.now());
    }

    public static Category reconstitute(String id, String name, String parentId, Instant createdAt) {
        return new Category(id, name, parentId, createdAt);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getParentId() { return parentId; }
    public Instant getCreatedAt() { return createdAt; }
}
