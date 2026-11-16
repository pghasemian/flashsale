package com.flashsale.catalog.api;

import com.flashsale.catalog.service.ProductService;
import com.flashsale.dto.common.ApiResponse;
import com.flashsale.dto.common.PageResponse;
import com.flashsale.dto.product.ProductCreateRequest;
import com.flashsale.dto.product.ProductResponse;
import com.flashsale.dto.product.ProductUpdateRequest;
import com.flashsale.dto.product.ProductPatchRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {

        ProductResponse product = productService.createProduct(request);
        return ApiResponse.success("Product created successfully", product);
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);

        return ApiResponse.success("Product retrieved successfully", product);
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ProductResponse> products = productService.getAllProducts(page, size);

        return ApiResponse.success("Products retrieved successfully", products);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {

        ProductResponse product =
                productService.updateProduct(id, request);

        return ApiResponse.success(
                "Product updated successfully", product);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {

        productService.deleteProduct(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ApiResponse<ProductResponse> patchProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductPatchRequest request) {

        ProductResponse product = productService.patchProduct(id, request);

        return ApiResponse.success("Product updated successfully", product);
    }
}