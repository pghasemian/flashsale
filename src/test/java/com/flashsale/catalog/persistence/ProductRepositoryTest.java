package com.flashsale.catalog.persistence;

import com.flashsale.catalog.domain.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldSaveAndFindProductBySku() {

        String sku = "SKU-" + UUID.randomUUID();

        Product product = Product.create(
                "Test Product",
                "Test Description",
                sku,
                new BigDecimal("100.00")
        );

        Product savedProduct = productRepository.save(product);

        Optional<Product> foundProduct =
                productRepository.findBySku(sku);

        assertThat(savedProduct.getId()).isNotNull();
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getName())
                .isEqualTo("Test Product");
        assertThat(foundProduct.get().getPrice())
                .isEqualByComparingTo("100.00");
        assertThat(foundProduct.get().isActive()).isTrue();
        assertThat(foundProduct.get().getCreatedAt()).isNotNull();
        assertThat(foundProduct.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldCheckIfProductExistsBySku() {

        String sku = "SKU-" + UUID.randomUUID();

        Product product = Product.create(
                "Test Product",
                null,
                sku,
                new BigDecimal("50.00")
        );

        productRepository.save(product);

        assertThat(productRepository.existsBySku(sku)).isTrue();
        assertThat(productRepository.existsBySku("UNKNOWN-SKU"))
                .isFalse();
    }
}