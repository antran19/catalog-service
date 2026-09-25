package com.nexus.catalog.api;

import com.nexus.catalog.api.dto.request.ChangeProductStatusRequest;
import com.nexus.catalog.api.dto.request.CreateProductRequest;
import com.nexus.catalog.api.dto.request.UpdateProductRequest;
import com.nexus.catalog.api.dto.response.ProductResponse;
import com.nexus.catalog.api.mapper.ProductApiMapper;
import com.nexus.catalog.application.usecase.ChangeProductStatusUseCase;
import com.nexus.catalog.application.usecase.CreateProductCommand;
import com.nexus.catalog.application.usecase.CreateProductUseCase;
import com.nexus.catalog.application.usecase.DeleteProductUseCase;
import com.nexus.catalog.application.usecase.GetProductUseCase;
import com.nexus.catalog.application.usecase.ProductResult;
import com.nexus.catalog.application.usecase.ProductSearchQuery;
import com.nexus.catalog.application.usecase.SearchProductsUseCase;
import com.nexus.catalog.application.usecase.UpdateProductUseCase;
import com.nexus.common.core.ApiResponse;
import com.nexus.common.security.RequiresPrivilege;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    // ADMIN-only privilege (user-service V5) that bypasses the per-product ownership check.
    private static final SimpleGrantedAuthority MANAGE_ANY = new SimpleGrantedAuthority("PRODUCT.MANAGE_ANY");

    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final GetProductUseCase getProductUseCase;
    private final SearchProductsUseCase searchProductsUseCase;
    private final ChangeProductStatusUseCase changeProductStatusUseCase;
    private final ProductApiMapper mapper;

    public ProductController(CreateProductUseCase createProductUseCase,
                              UpdateProductUseCase updateProductUseCase,
                              DeleteProductUseCase deleteProductUseCase,
                              GetProductUseCase getProductUseCase,
                              SearchProductsUseCase searchProductsUseCase,
                              ChangeProductStatusUseCase changeProductStatusUseCase,
                              ProductApiMapper mapper) {
        this.createProductUseCase = createProductUseCase;
        this.updateProductUseCase = updateProductUseCase;
        this.deleteProductUseCase = deleteProductUseCase;
        this.getProductUseCase = getProductUseCase;
        this.searchProductsUseCase = searchProductsUseCase;
        this.changeProductStatusUseCase = changeProductStatusUseCase;
        this.mapper = mapper;
    }

    @RequiresPrivilege("PRODUCT.CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(Authentication authentication,
                                                                 @Valid @RequestBody CreateProductRequest request) {
        // sellerId MUST come from the authenticated JWT principal, never from the request body:
        // a client must not be able to create a product under another seller's identity by
        // putting an arbitrary sellerId in the JSON payload. CreateProductRequest intentionally
        // has no sellerId field at all.
        String sellerId = callerId(authentication);
        List<String> images = request.imageUrls() == null ? List.of() : request.imageUrls();
        CreateProductCommand command = new CreateProductCommand(
                request.name(), request.description(), request.categoryId(), sellerId, request.price(), images);
        ProductResult result = createProductUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(mapper.toResponse(result)));
    }

    @RequiresPrivilege("PRODUCT.UPDATE")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(Authentication authentication,
                                                                 @PathVariable String id,
                                                                 @Valid @RequestBody UpdateProductRequest request) {
        ProductResult result = updateProductUseCase.update(id, request.name(), request.description(),
                callerId(authentication), canManageAny(authentication));
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    @RequiresPrivilege("PRODUCT.DELETE")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable String id) {
        deleteProductUseCase.delete(id, callerId(authentication), canManageAny(authentication));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // Publishing/unpublishing a listing (DRAFT -> ACTIVE -> INACTIVE). Gated by PRODUCT.UPDATE plus
    // the same owner-or-MANAGE_ANY check as update/delete (enforced in the use case).
    @RequiresPrivilege("PRODUCT.UPDATE")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> changeStatus(Authentication authentication,
                                                                       @PathVariable String id,
                                                                       @Valid @RequestBody ChangeProductStatusRequest request) {
        ProductResult result = changeProductStatusUseCase.changeStatus(id, request.status(),
                callerId(authentication), canManageAny(authentication));
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ProductResult> results = searchProductsUseCase.search(new ProductSearchQuery(q, categoryId, status, page, size));
        List<ProductResponse> responses = results.stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/discover")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> discover(
            @RequestParam(required = false) String categoryId) {
        List<ProductResult> results = searchProductsUseCase.search(
                new ProductSearchQuery(null, categoryId, "ACTIVE", 0, 20));
        List<ProductResponse> responses = results.stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> get(@PathVariable String id) {
        ProductResult result = getProductUseCase.get(id);
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    // JwtAuthenticationFilter sets the JWT subject (the caller's user id) as the principal.
    private static String callerId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }

    // The ownership decision itself lives in the use cases (unit-testable without MockMvc); the
    // controller only translates the authenticated principal into the two facts they need.
    private static boolean canManageAny(Authentication authentication) {
        return authentication.getAuthorities().contains(MANAGE_ANY);
    }
}
