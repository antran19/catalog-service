package com.nexus.catalog.application.usecase;

public record CreateCategoryCommand(String name, String parentId) {
}
