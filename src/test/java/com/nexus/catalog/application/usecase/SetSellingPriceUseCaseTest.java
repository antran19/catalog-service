package com.nexus.catalog.application.usecase;

import com.nexus.catalog.application.exception.ProductNotFoundException;
import com.nexus.catalog.application.port.out.ProductImagePort;
import com.nexus.catalog.application.port.out.ProductRepositoryPort;
import com.nexus.catalog.application.port.out.SellingPriceRepositoryPort;
import com.nexus.catalog.application.port.out.SkuRepositoryPort;
import com.nexus.catalog.domain.model.Product;
import com.nexus.catalog.domain.model.ProductStatus;
import com.nexus.catalog.domain.model.Sku;
import com.nexus.common.core.exception.ForbiddenException;
import com.nexus.common.core.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SetSellingPriceUseCaseTest {

    private ProductRepositoryPort productRepositoryPort;
    private SkuRepositoryPort skuRepositoryPort;
    private SellingPriceRepositoryPort sellingPriceRepositoryPort;
    private ProductImagePort productImagePort;
    private SetSellingPriceUseCase useCase;

    @BeforeEach
    void setUp() {
        productRepositoryPort = mock(ProductRepositoryPort.class);
        skuRepositoryPort = mock(SkuRepositoryPort.class);
        sellingPriceRepositoryPort = mock(SellingPriceRepositoryPort.class);
        productImagePort = mock(ProductImagePort.class);
        useCase = new SetSellingPriceUseCase(productRepositoryPort, skuRepositoryPort,
                sellingPriceRepositoryPort, productImagePort);

        Product product = Product.reconstitute("p-id", "Ao", "desc", "cat-id",
                ProductStatus.ACTIVE, "seller-id", Instant.now(), Instant.now());
        when(productRepositoryPort.findById("p-id")).thenReturn(Optional.of(product));
        when(skuRepositoryPort.findByProductId("p-id")).thenReturn(List.of(
                Sku.reconstitute("sku-id", "p-id", "SKU-1", new BigDecimal("400000"), new BigDecimal("400000"), null)));
        when(skuRepositoryPort.save(any(Sku.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productImagePort.findUrlsByProductId("p-id")).thenReturn(List.of());
    }

    @Test
    void ownerRaisesTheSellingPrice_itIsSavedAndRecordedInTheHistory() {
        ProductResult result = useCase.setSellingPrice("p-id", new BigDecimal("550000"), "seller-id", false);

        assertThat(result.price()).isEqualByComparingTo("550000");
        verify(skuRepositoryPort).save(argThat(s -> s.getPrice().compareTo(new BigDecimal("550000")) == 0
                && s.getOriginalPrice().compareTo(new BigDecimal("400000")) == 0));
        verify(sellingPriceRepositoryPort).record("sku-id", new BigDecimal("550000"), "seller-id");
    }

    @Test
    void sellingAtExactlyTheOriginalPrice_isAllowed() {
        ProductResult result = useCase.setSellingPrice("p-id", new BigDecimal("400000"), "seller-id", false);

        assertThat(result.price()).isEqualByComparingTo("400000");
    }

    @Test
    void sellingBelowTheOriginalPrice_isRefusedAndNothingIsSaved() {
        assertThatThrownBy(() -> useCase.setSellingPrice("p-id", new BigDecimal("399999"), "seller-id", false))
                .isInstanceOf(ValidationException.class);

        verify(skuRepositoryPort, never()).save(any());
        verifyNoInteractions(sellingPriceRepositoryPort);
    }

    @Test
    void anotherSellerCannotSetThePrice() {
        assertThatThrownBy(() -> useCase.setSellingPrice("p-id", new BigDecimal("550000"), "someone-else", false))
                .isInstanceOf(ForbiddenException.class);

        verify(skuRepositoryPort, never()).save(any());
    }

    @Test
    void anAdminWithManageAnyCanSetThePrice() {
        ProductResult result = useCase.setSellingPrice("p-id", new BigDecimal("550000"), "admin-id", true);

        assertThat(result.price()).isEqualByComparingTo("550000");
        verify(sellingPriceRepositoryPort).record("sku-id", new BigDecimal("550000"), "admin-id");
    }

    @Test
    void unknownProduct_isNotFound() {
        when(productRepositoryPort.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.setSellingPrice("nope", new BigDecimal("550000"), "seller-id", false))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
