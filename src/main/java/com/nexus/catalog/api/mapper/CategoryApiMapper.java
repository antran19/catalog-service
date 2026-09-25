package com.nexus.catalog.api.mapper;

import com.nexus.catalog.api.dto.request.CreateCategoryRequest;
import com.nexus.catalog.api.dto.response.CategoryResponse;
import com.nexus.catalog.application.usecase.CategoryResult;
import com.nexus.catalog.application.usecase.CreateCategoryCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryApiMapper {
    CreateCategoryCommand toCommand(CreateCategoryRequest request);
    CategoryResponse toResponse(CategoryResult result);
}
