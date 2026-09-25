package com.nexus.catalog.api.mapper;

import com.nexus.catalog.api.dto.response.ProductResponse;
import com.nexus.catalog.application.usecase.ProductResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductApiMapper {
    ProductResponse toResponse(ProductResult result);
}
