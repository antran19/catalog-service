package com.nexus.catalog.api;

import com.nexus.catalog.api.mapper.CategoryApiMapperImpl;
import com.nexus.catalog.application.usecase.CategoryResult;
import com.nexus.catalog.application.usecase.CreateCategoryUseCase;
import com.nexus.catalog.application.usecase.GetCategoryUseCase;
import com.nexus.catalog.application.usecase.ListCategoriesUseCase;
import com.nexus.catalog.application.usecase.UpdateCategoryUseCase;
import com.nexus.catalog.application.usecase.DeleteCategoryUseCase;
import com.nexus.catalog.infrastructure.config.SecurityConfig;
import com.nexus.common.security.JwtAuthenticationFilter;
import com.nexus.common.security.JwtTokenProvider;
import com.nexus.common.web.GlobalExceptionHandler;
import com.nexus.common.security.PrivilegeAuthorizationAspect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
// AopAutoConfiguration: @WebMvcTest does not enable AspectJ auto-proxying on its own, so without it
// PrivilegeAuthorizationAspect would be registered but never applied and @RequiresPrivilege would be
// silently unenforced in this slice.
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import({GlobalExceptionHandler.class, CategoryApiMapperImpl.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, PrivilegeAuthorizationAspect.class})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private CreateCategoryUseCase createCategoryUseCase;
    @MockBean private UpdateCategoryUseCase updateCategoryUseCase;
    @MockBean private DeleteCategoryUseCase deleteCategoryUseCase;
    @MockBean private GetCategoryUseCase getCategoryUseCase;
    @MockBean private ListCategoriesUseCase listCategoriesUseCase;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    void listCategories_isPublicAndReturns200WithoutAToken() throws Exception {
        when(listCategoriesUseCase.listAll()).thenReturn(List.of(new CategoryResult("id-1", "Electronics", null)));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Electronics"));
    }

    @Test
    void createCategory_returns401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Electronics","parentId":null}"""))
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
    void createCategory_returns403WithoutCategoryCreatePrivilege() throws Exception {
        authenticateAs("buyer-id", "PROFILE.VIEW");

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Electronics","parentId":null}"""))
                .andExpect(status().isForbidden());

        verifyNoInteractions(createCategoryUseCase);
    }
}
