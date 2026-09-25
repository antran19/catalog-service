package com.nexus.catalog.api;

import com.nexus.catalog.api.dto.request.CreateCategoryRequest;
import com.nexus.catalog.api.dto.request.UpdateCategoryRequest;
import com.nexus.catalog.api.dto.response.CategoryResponse;
import com.nexus.catalog.api.mapper.CategoryApiMapper;
import com.nexus.catalog.application.usecase.*;
import com.nexus.common.core.ApiResponse;
import com.nexus.common.security.RequiresPrivilege;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;
    private final GetCategoryUseCase getCategoryUseCase;
    private final ListCategoriesUseCase listCategoriesUseCase;
    private final CategoryApiMapper mapper;

    public CategoryController(CreateCategoryUseCase createCategoryUseCase,
                               UpdateCategoryUseCase updateCategoryUseCase,
                               DeleteCategoryUseCase deleteCategoryUseCase,
                               GetCategoryUseCase getCategoryUseCase,
                               ListCategoriesUseCase listCategoriesUseCase,
                               CategoryApiMapper mapper) {
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
        this.getCategoryUseCase = getCategoryUseCase;
        this.listCategoriesUseCase = listCategoriesUseCase;
        this.mapper = mapper;
    }

    @RequiresPrivilege("CATEGORY.CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResult result = createCategoryUseCase.create(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(mapper.toResponse(result)));
    }

    @RequiresPrivilege("CATEGORY.UPDATE")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable String id,
                                                                  @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryResult result = updateCategoryUseCase.update(id, request.name());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    @RequiresPrivilege("CATEGORY.DELETE")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        deleteCategoryUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> get(@PathVariable String id) {
        CategoryResult result = getCategoryUseCase.get(id);
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> list() {
        List<CategoryResponse> responses = listCategoriesUseCase.listAll().stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }
}
