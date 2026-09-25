package com.nexus.catalog.infrastructure.config;

import com.nexus.catalog.application.port.out.CategoryRepositoryPort;
import com.nexus.catalog.application.port.out.EventPublisherPort;
import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.application.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public CreateCategoryUseCase createCategoryUseCase(CategoryRepositoryPort port, EventPublisherPort eventPublisherPort) {
        return new CreateCategoryUseCase(port, eventPublisherPort);
    }

    @Bean
    public UpdateCategoryUseCase updateCategoryUseCase(CategoryRepositoryPort port, EventPublisherPort eventPublisherPort) {
        return new UpdateCategoryUseCase(port, eventPublisherPort);
    }

    @Bean
    public DeleteCategoryUseCase deleteCategoryUseCase(CategoryRepositoryPort categoryPort,
                                                         ProductRepositoryPort productPort) {
        return new DeleteCategoryUseCase(categoryPort, productPort);
    }

    @Bean
    public GetCategoryUseCase getCategoryUseCase(CategoryRepositoryPort port) {
        return new GetCategoryUseCase(port);
    }

    @Bean
    public ListCategoriesUseCase listCategoriesUseCase(CategoryRepositoryPort port) {
        return new ListCategoriesUseCase(port);
    }

    @Bean
    public CreateProductUseCase createProductUseCase(ProductRepositoryPort productPort,
                                                       SkuRepositoryPort skuPort,
                                                       ProductImagePort imagePort,
                                                       CategoryRepositoryPort categoryPort,
                                                       EventPublisherPort eventPublisherPort) {
        return new CreateProductUseCase(productPort, skuPort, imagePort, categoryPort, eventPublisherPort);
    }

    @Bean
    public UpdateProductUseCase updateProductUseCase(ProductRepositoryPort productPort,
                                                       SkuRepositoryPort skuPort,
                                                       ProductImagePort imagePort,
                                                       EventPublisherPort eventPublisherPort) {
        return new UpdateProductUseCase(productPort, skuPort, imagePort, eventPublisherPort);
    }

    @Bean
    public DeleteProductUseCase deleteProductUseCase(ProductRepositoryPort productPort,
                                                       SkuRepositoryPort skuPort,
                                                       ProductImagePort imagePort) {
        return new DeleteProductUseCase(productPort, skuPort, imagePort);
    }

    @Bean
    public ChangeProductStatusUseCase changeProductStatusUseCase(ProductRepositoryPort productPort,
                                                                   SkuRepositoryPort skuPort,
                                                                   ProductImagePort imagePort,
                                                                   EventPublisherPort eventPublisherPort) {
        return new ChangeProductStatusUseCase(productPort, skuPort, imagePort, eventPublisherPort);
    }

    @Bean
    public GetProductUseCase getProductUseCase(ProductRepositoryPort productPort,
                                                SkuRepositoryPort skuPort,
                                                ProductImagePort imagePort) {
        return new GetProductUseCase(productPort, skuPort, imagePort);
    }

    @Bean
    public SearchProductsUseCase searchProductsUseCase(ProductRepositoryPort productPort,
                                                         SkuRepositoryPort skuPort,
                                                         ProductImagePort imagePort) {
        return new SearchProductsUseCase(productPort, skuPort, imagePort);
    }
}
