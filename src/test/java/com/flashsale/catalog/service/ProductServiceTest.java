package com.flashsale.catalog.service;

import com.flashsale.catalog.domain.Product;
import com.flashsale.catalog.persistence.ProductRepository;
import com.flashsale.dto.common.PageResponse;
import com.flashsale.dto.product.ProductCreateRequest;
import com.flashsale.dto.product.ProductPatchRequest;
import com.flashsale.dto.product.ProductResponse;
import com.flashsale.dto.product.ProductUpdateRequest;
import com.flashsale.exception.DuplicateResourceException;
import com.flashsale.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProductSuccessfully() {

        ProductCreateRequest request = new ProductCreateRequest(
                "Laptop",
                "LAPTOP-001",
                "Gaming laptop",
                new BigDecimal("1500.00")
        );

        when(productRepository.existsBySku("LAPTOP-001"))
                .thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.createProduct(request);

        assertEquals("Laptop", response.name());
        assertEquals("LAPTOP-001", response.sku());
        assertEquals(new BigDecimal("1500.00"), response.price());
        assertEquals(true, response.active());
    }

    @Test
    void shouldThrowExceptionWhenSkuAlreadyExists() {

        ProductCreateRequest request = new ProductCreateRequest(
                "Laptop",
                "LAPTOP-001",
                "Gaming laptop",
                new BigDecimal("1500.00")
        );

        when(productRepository.existsBySku("LAPTOP-001"))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> productService.createProduct(request)
        );

        verify(productRepository, never())
                .save(any(Product.class));
    }

    @Test
    void shouldGetProductByIdSuccessfully() {

        Product product = Product.create(
                "Laptop",
                "Gaming laptop",
                "LAPTOP-001",
                new BigDecimal("1500.00")
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertEquals("Laptop", response.name());
        assertEquals("LAPTOP-001", response.sku());
        assertEquals(new BigDecimal("1500.00"), response.price());

        verify(productRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenProductDoesNotExist() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProductById(999L)
        );

        verify(productRepository).findById(999L);
    }

    @Test
    void shouldGetAllActiveProductsWithPagination() {

        Product product1 = Product.create(
                "Laptop",
                "Gaming laptop",
                "LAPTOP-001",
                new BigDecimal("1500.00")
        );

        Product product2 = Product.create(
                "Phone",
                "Smartphone",
                "PHONE-001",
                new BigDecimal("800.00")
        );

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        PageImpl<Product> productPage = new PageImpl<>(
                List.of(product1, product2),
                pageable,
                2
        );

        when(productRepository.findAllByActiveTrue(any(Pageable.class)))
                .thenReturn(productPage);

        PageResponse<ProductResponse> response =
                productService.getAllProducts(0, 10);

        assertEquals(2, response.content().size());
        assertEquals(0, response.page());
        assertEquals(10, response.size());
        assertEquals(2, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals(true, response.last());

        verify(productRepository)
                .findAllByActiveTrue(any(Pageable.class));
    }

    @Test
    void shouldUpdateProductSuccessfully() {

        Product product = Product.create(
                "Laptop",
                "Old description",
                "LAPTOP-001",
                new BigDecimal("1500.00")
        );

        ProductUpdateRequest request = new ProductUpdateRequest(
                "Gaming Laptop",
                "LAPTOP-002",
                "Updated description",
                new BigDecimal("1800.00")
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(productRepository.existsBySku("LAPTOP-002"))
                .thenReturn(false);

        ProductResponse response =
                productService.updateProduct(1L, request);

        assertEquals("Gaming Laptop", response.name());
        assertEquals("LAPTOP-002", response.sku());
        assertEquals("Updated description", response.description());
        assertEquals(new BigDecimal("1800.00"), response.price());

        verify(productRepository).findById(1L);
        verify(productRepository).save(product);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingProductWithExistingSku() {

        Product product = Product.create(
                "Laptop",
                "Gaming laptop",
                "LAPTOP-001",
                new BigDecimal("1500.00")
        );

        ProductUpdateRequest request = new ProductUpdateRequest(
                "Gaming Laptop",
                "LAPTOP-002",
                "Updated description",
                new BigDecimal("1800.00")
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.existsBySku("LAPTOP-002"))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> productService.updateProduct(1L, request)
        );

        verify(productRepository).findById(1L);
        verify(productRepository).existsBySku("LAPTOP-002");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistingProduct() {

        ProductUpdateRequest request = new ProductUpdateRequest(
                "Gaming Laptop",
                "LAPTOP-002",
                "Updated description",
                new BigDecimal("1800.00")
        );

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.updateProduct(999L, request)
        );

        verify(productRepository).findById(999L);

        verify(productRepository, never()).save(any(Product.class));

        verify(productRepository, never()).existsBySku(any(String.class));
    }

    @Test
    void shouldSoftDeleteProductSuccessfully() {

        Product product = Product.create(
                "Laptop",
                "Gaming laptop",
                "LAPTOP-001",
                new BigDecimal("1500.00")
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        productService.deleteProduct(1L);

        assertEquals(false, product.isActive());

        verify(productRepository).findById(1L);
        verify(productRepository).save(product);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingProduct() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.deleteProduct(999L)
        );

        verify(productRepository).findById(999L);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingInactiveProduct() {

        Product product = Product.create(
                "Laptop",
                "Gaming laptop",
                "LAPTOP-001",
                new BigDecimal("1500.00")
        );

        product.setActive(false);

        ProductUpdateRequest request = new ProductUpdateRequest(
                "Gaming Laptop Pro",
                "LAPTOP-001",
                "Updated description",
                new BigDecimal("1800.00")
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.updateProduct(1L, request)
        );

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenPatchingInactiveProduct() {

        Product product = Product.create(
                "Laptop",
                "Gaming laptop",
                "LAPTOP-001",
                new BigDecimal("1500.00")
        );

        product.setActive(false);

        ProductPatchRequest request = new ProductPatchRequest(
                null,
                null,
                null,
                new BigDecimal("1800.00")
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.patchProduct(1L, request)
        );

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenPatchingProductWithExistingSku() {

        Product product = Product.create(
                "Laptop",
                "Gaming laptop",
                "LAPTOP-001",
                new BigDecimal("1500.00")
        );

        ProductPatchRequest request = new ProductPatchRequest(
                null,
                "LAPTOP-002",
                null,
                null
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.existsBySku("LAPTOP-002"))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> productService.patchProduct(1L, request)
        );

        verify(productRepository).findById(1L);

        verify(productRepository).existsBySku("LAPTOP-002");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenPatchingNonExistingProduct() {

        ProductPatchRequest request = new ProductPatchRequest(
                "Gaming Laptop Pro",
                null,
                null,
                new BigDecimal("1800.00")
        );

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.patchProduct(999L, request)
        );

        verify(productRepository).findById(999L);

        verify(productRepository, never()).save(any(Product.class));

        verify(productRepository, never()).existsBySku(any(String.class));
    }
}