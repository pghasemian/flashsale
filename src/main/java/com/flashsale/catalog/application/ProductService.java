package com.flashsale.catalog.application;

import com.flashsale.catalog.domain.Product;
import com.flashsale.catalog.persistence.ProductRepository;
import com.flashsale.dto.common.PageResponse;
import com.flashsale.dto.product.ProductCreateRequest;
import com.flashsale.dto.product.ProductPatchRequest;
import com.flashsale.dto.product.ProductResponse;
import com.flashsale.dto.product.ProductUpdateRequest;
import com.flashsale.exception.DuplicateResourceException;
import com.flashsale.exception.ResourceNotFoundException;
import com.flashsale.mapper.ProductMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {

        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException(
                    "Product with SKU " + request.sku() + " already exists"
            );
        }

        Product product = Product.create(
                request.name(),
                request.description(),
                request.sku(),
                request.price()
        );

        Product savedProduct = productRepository.save(product);

        return ProductMapper.toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id
                ));

        return ProductMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ProductResponse> productPage = productRepository
                .findAllByActiveTrue(pageable)
                .map(ProductMapper::toResponse);

        return PageResponse.from(productPage);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id
                ));

        if (!product.getSku().equals(request.sku())
                && productRepository.existsBySku(request.sku())) {

            throw new DuplicateResourceException(
                    "Product with SKU " + request.sku() + " already exists");
        }

        if (!product.isActive()) {
            throw new ResourceNotFoundException(
                    "Product not found with id: " + id
            );
        }

        product.setName(request.name());
        product.setDescription(request.description());
        product.setSku(request.sku());
        product.setPrice(request.price());

        Product updatedProduct = productRepository.save(product);

        return ProductMapper.toResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id
                ));

        product.setActive(false);

        productRepository.save(product);
    }

    @Transactional
    public ProductResponse patchProduct(Long id, ProductPatchRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id
                ));

        if (request.sku() != null
                && !product.getSku().equals(request.sku())
                && productRepository.existsBySku(request.sku())) {

            throw new DuplicateResourceException(
                    "Product with SKU " + request.sku() + " already exists");
        }

        if (!product.isActive()) {
            throw new ResourceNotFoundException(
                    "Product not found with id: " + id
            );
        }

        if (request.name() != null) {
            product.setName(request.name());
        }

        if (request.description() != null) {
            product.setDescription(request.description());
        }

        if (request.sku() != null) {
            product.setSku(request.sku());
        }

        if (request.price() != null) {
            product.setPrice(request.price());
        }

        Product updatedProduct = productRepository.save(product);

        return ProductMapper.toResponse(updatedProduct);
    }
}