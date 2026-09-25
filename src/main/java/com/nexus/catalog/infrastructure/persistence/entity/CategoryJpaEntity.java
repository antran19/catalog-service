package com.nexus.catalog.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class CategoryJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CategoryJpaEntity() {
    }

    public CategoryJpaEntity(UUID id, String name, UUID parentId, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public UUID getParentId() { return parentId; }
    public Instant getCreatedAt() { return createdAt; }
}
