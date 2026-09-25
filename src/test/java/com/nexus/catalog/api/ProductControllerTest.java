package com.nexus.catalog.api;

import com.nexus.catalog.api.mapper.ProductApiMapperImpl;
import com.nexus.catalog.application.usecase.ChangeProductStatusUseCase;
import com.nexus.catalog.application.usecase.CreateProductCommand;
import com.nexus.catalog.application.usecase.CreateProductUseCase;
import com.nexus.catalog.application.usecase.DeleteProductUseCase;
import com.nexus.catalog.application.usecase.GetProductUseCase;
import com.nexus.catalog.application.usecase.ProductResult;
import com.nexus.catalog.application.usecase.ProductSearchQuery;
import com.nexus.catalog.application.usecase.SearchProductsUseCase;
import com.nexus.catalog.application.usecase.UpdateProductUseCase;
import com.nexus.catalog.infrastructure.config.SecurityConfig;
import com.nexus.common.core.exception.ForbiddenException;
import com.nexus.common.security.JwtAuthenticationFilter;
import com.nexus.common.security.JwtTokenProvider;
import com.nexus.common.security.PrivilegeAuthorizationAspect;
import com.nexus.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
// AopAutoConfiguration: @WebMvcTest does not enable AspectJ auto-proxying on its own, so without it
// PrivilegeAuthorizationAspect would be registered but never applied and @RequiresPrivilege would be
// silently unenforced in this slice.
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import({GlobalExceptionHandler.class, ProductApiMapperImpl.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, PrivilegeAuthorizationAspect.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private CreateProductUseCase createProductUseCase;
    @MockBean private UpdateProductUseCase updateProductUseCase;
    @MockBean private DeleteProductUseCase deleteProductUseCase;
    @MockBean private GetProductUseCase getProductUseCase;
    @MockBean private SearchProductsUseCase searchProductsUseCase;
    @MockBean private ChangeProductStatusUseCase changeProductStatusUseCase;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    void createProduct_returns401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Laptop X","description":"desc","categoryId":"cat-id",
                                 "price":999.99,"imageUrls":[]}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_returns201WithValidTokenAndPrivilege() throws Exception {
        when(jwtTokenProvider.isValid("good-token")).thenReturn(true);
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                .subject("seller-id")
                .add("privileges", List.of("PRODUCT.CREATE"))
                .build();
        when(jwtTokenProvider.parseClaims("good-token")).thenReturn(claims);
        when(createProductUseCase.create(any())).thenReturn(new ProductResult(
                "product-id", "Laptop X", "desc", "cat-id", "DRAFT", "seller-id",
                List.of(), "sku-id", "SKU-ABC12345", new BigDecimal("999.99")));

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Laptop X","description":"desc","categoryId":"cat-id",
                                 "price":999.99,"imageUrls":[]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Laptop X"));

        verify(createProductUseCase).create(argThat(
                (CreateProductCommand cmd) -> cmd.sellerId().equals("seller-id")));
    }

    @Test
    void createProduct_ignoresSpoofedSellerIdInRequestBody_usesJwtSubjectInstead() throws Exception {
        when(jwtTokenProvider.isValid("good-token")).thenReturn(true);
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                .subject("seller-id")
                .add("privileges", List.of("PRODUCT.CREATE"))
                .build();
        when(jwtTokenProvider.parseClaims("good-token")).thenReturn(claims);
        when(createProductUseCase.create(any())).thenReturn(new ProductResult(
                "product-id", "Laptop X", "desc", "cat-id", "DRAFT", "seller-id",
                List.of(), "sku-id", "SKU-ABC12345", new BigDecimal("999.99")));

        // Attacker tries to smuggle a different sellerId via the JSON payload.
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Laptop X","description":"desc","categoryId":"cat-id",
                                 "sellerId":"attacker-id","price":999.99,"imageUrls":[]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sellerId").value("seller-id"));

        // The use case must have been invoked with the JWT subject, never the spoofed value.
        verify(createProductUseCase).create(argThat(
                (CreateProductCommand cmd) -> cmd.sellerId().equals("seller-id")
                        && !cmd.sellerId().equals("attacker-id")));
    }

    @Test
    void deleteProduct_returns401WithoutToken() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/products/p-id"))
                .andExpect(status().isUnauthorized());
    }

    private void authenticateAs(String subject, String... privileges) {
        when(jwtTokenProvider.isValid("good-token")).thenReturn(true);
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                .subject(subject)
                .add("privileges", List.of(privileges))
                .build();
        when(jwtTokenProvider.parseClaims("good-token")).thenReturn(claims);
    }

    @Test
    void updateProduct_returns200AndPassesCallerIdentityToUseCase() throws Exception {
        authenticateAs("seller-id", "PRODUCT.UPDATE");
        when(updateProductUseCase.update("p-id", "Laptop X Updated", "desc", "seller-id", false))
                .thenReturn(new ProductResult(
                        "p-id", "Laptop X Updated", "desc", "cat-id", "DRAFT", "seller-id",
                        List.of(), "sku-id", "SKU-ABC12345", new BigDecimal("999.99")));

        // Also the regression test for path-variable binding: deleteProduct_returns401WithoutToken
        // never reaches the handler (Spring Security rejects it first), so only an authenticated
        // request proves {id} actually binds.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/products/p-id")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Laptop X Updated","description":"desc"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Laptop X Updated"));

        // The caller id must come from the JWT subject, and canManageAny must be false for a
        // caller who does not hold PRODUCT.MANAGE_ANY.
        verify(updateProductUseCase).update("p-id", "Laptop X Updated", "desc", "seller-id", false);
    }

    @Test
    void updateProduct_passesCanManageAnyTrueWhenCallerHoldsManageAny() throws Exception {
        authenticateAs("admin-id", "PRODUCT.UPDATE", "PRODUCT.MANAGE_ANY");
        when(updateProductUseCase.update("p-id", "Moderated", "desc", "admin-id", true))
                .thenReturn(new ProductResult(
                        "p-id", "Moderated", "desc", "cat-id", "DRAFT", "seller-id",
                        List.of(), "sku-id", "SKU-ABC12345", new BigDecimal("999.99")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/products/p-id")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Moderated","description":"desc"}"""))
                .andExpect(status().isOk());

        verify(updateProductUseCase).update("p-id", "Moderated", "desc", "admin-id", true);
    }

    @Test
    void updateProduct_returns403WhenUseCaseRejectsNonOwner() throws Exception {
        authenticateAs("other-seller", "PRODUCT.UPDATE");
        when(updateProductUseCase.update("p-id", "Hijacked", "desc", "other-seller", false))
                .thenThrow(new ForbiddenException("PRODUCT_NOT_OWNED", "not yours"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/products/p-id")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Hijacked","description":"desc"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_OWNED"));
    }

    @Test
    void deleteProduct_returns200AndPassesCallerIdentityToUseCase() throws Exception {
        authenticateAs("seller-id", "PRODUCT.DELETE");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/products/p-id")
                        .header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk());

        verify(deleteProductUseCase).delete("p-id", "seller-id", false);
    }

    @Test
    void deleteProduct_passesCanManageAnyTrueWhenCallerHoldsManageAny() throws Exception {
        authenticateAs("admin-id", "PRODUCT.DELETE", "PRODUCT.MANAGE_ANY");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/products/p-id")
                        .header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk());

        verify(deleteProductUseCase).delete("p-id", "admin-id", true);
    }

    @Test
    void deleteProduct_returns403WhenUseCaseRejectsNonOwner() throws Exception {
        authenticateAs("other-seller", "PRODUCT.DELETE");
        doThrow(new ForbiddenException("PRODUCT_NOT_OWNED", "not yours"))
                .when(deleteProductUseCase).delete("p-id", "other-seller", false);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/products/p-id")
                        .header("Authorization", "Bearer good-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProduct_isPublicAndReturns200WithoutAToken() throws Exception {
        when(getProductUseCase.get("p-id")).thenReturn(new ProductResult(
                "p-id", "Laptop X", "desc", "cat-id", "DRAFT", "seller-id",
                List.of(), "sku-id", "SKU-ABC12345", new BigDecimal("999.99")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/products/p-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Laptop X"));
    }

    @Test
    void discover_isPublicAndDelegatesToSearchWithActiveStatusOnly() throws Exception {
        when(searchProductsUseCase.search(any())).thenReturn(List.of());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/products/discover"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductSearchQuery> captor = ArgumentCaptor.forClass(ProductSearchQuery.class);
        verify(searchProductsUseCase).search(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("ACTIVE");
        assertThat(captor.getValue().q()).isNull();
    }

    @Test
    void changeStatus_returns401WithoutToken() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/products/p-id/status")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"status":"ACTIVE"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changeStatus_returns200AndPassesCallerIdentityToUseCase() throws Exception {
        authenticateAs("seller-id", "PRODUCT.UPDATE");
        when(changeProductStatusUseCase.changeStatus("p-id", "ACTIVE", "seller-id", false))
                .thenReturn(new ProductResult(
                        "p-id", "Laptop X", "desc", "cat-id", "ACTIVE", "seller-id",
                        List.of(), "sku-id", "SKU-ABC12345", new BigDecimal("999.99")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/products/p-id/status")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"status":"ACTIVE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(changeProductStatusUseCase).changeStatus("p-id", "ACTIVE", "seller-id", false);
    }

    @Test
    void changeStatus_passesCanManageAnyTrueWhenCallerHoldsManageAny() throws Exception {
        authenticateAs("admin-id", "PRODUCT.UPDATE", "PRODUCT.MANAGE_ANY");
        when(changeProductStatusUseCase.changeStatus("p-id", "INACTIVE", "admin-id", true))
                .thenReturn(new ProductResult(
                        "p-id", "Laptop X", "desc", "cat-id", "INACTIVE", "seller-id",
                        List.of(), "sku-id", "SKU-ABC12345", new BigDecimal("999.99")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/products/p-id/status")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}"""))
                .andExpect(status().isOk());

        verify(changeProductStatusUseCase).changeStatus("p-id", "INACTIVE", "admin-id", true);
    }

    @Test
    void changeStatus_returns403WithoutProductUpdatePrivilege() throws Exception {
        authenticateAs("buyer-id", "PROFILE.VIEW");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/products/p-id/status")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"status":"ACTIVE"}"""))
                .andExpect(status().isForbidden());

        verifyNoInteractions(changeProductStatusUseCase);
    }

    @Test
    void changeStatus_returns400WhenStatusMissing() throws Exception {
        authenticateAs("seller-id", "PRODUCT.UPDATE");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/products/p-id/status")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(changeProductStatusUseCase);
    }

    // Exercises bare @RequestParam binding (relies on maven.compiler.parameters=true, now the
    // project-wide convention instead of explicit @RequestParam(name = "...") values).
    @Test
    void search_bindsAllQueryParamsByName() throws Exception {
        when(searchProductsUseCase.search(any())).thenReturn(List.of());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/products")
                        .param("q", "laptop")
                        .param("categoryId", "11111111-1111-1111-1111-111111111111")
                        .param("status", "ACTIVE")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductSearchQuery> captor = ArgumentCaptor.forClass(ProductSearchQuery.class);
        verify(searchProductsUseCase).search(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new ProductSearchQuery(
                "laptop", "11111111-1111-1111-1111-111111111111", "ACTIVE", 2, 5));
    }

    @Test
    void discover_bindsCategoryIdParamByName() throws Exception {
        when(searchProductsUseCase.search(any())).thenReturn(List.of());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/products/discover")
                        .param("categoryId", "11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductSearchQuery> captor = ArgumentCaptor.forClass(ProductSearchQuery.class);
        verify(searchProductsUseCase).search(captor.capture());
        assertThat(captor.getValue().categoryId()).isEqualTo("11111111-1111-1111-1111-111111111111");
    }
}