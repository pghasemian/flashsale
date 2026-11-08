package com.flashsale.exception;

import com.flashsale.dto.product.ProductCreateRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(
                        new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean()
                )
                .build();
    }

    @Test
    void shouldReturn404WhenResourceNotFound() throws Exception {

        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Product not found"))
                .andExpect(jsonPath("$.path")
                        .value("/test/not-found"));
    }

    @Test
    void shouldReturn409WhenResourceIsDuplicated() throws Exception {

        mockMvc.perform(get("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Product SKU already exists"));
    }

    @Test
    void shouldReturn500WhenUnexpectedExceptionOccurs() throws Exception {

        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Internal server error"));
    }

    @Test
    void shouldReturn400WhenProductValidationFails() throws Exception {

        String requestBody = """
            {
                "name": "",
                "sku": "LAPTOP-001",
                "description": "Test product",
                "price": 0.00
            }
            """;

        mockMvc.perform(post("/test/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void shouldReturn400WhenRequestBodyIsInvalid() throws Exception {

        String invalidJson = """
            {
                "name": "Laptop",
                "sku": "LAPTOP-001",
                "price":
            """;

        mockMvc.perform(post("/test/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Invalid request body"));
    }

    @Test
    void shouldNotExposeInternalExceptionMessage() throws Exception {

        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Internal server error"))
                .andExpect(jsonPath("$.errors[0]")
                        .value("An unexpected error occurred"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(
                                org.hamcrest.Matchers.not(
                                        org.hamcrest.Matchers.containsString(
                                                "Unexpected error"
                                        )
                                )
                        ));
    }

    @org.springframework.web.bind.annotation.RestController
    static class TestController {

        @org.springframework.web.bind.annotation.GetMapping("/test/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Product not found");
        }

        @org.springframework.web.bind.annotation.GetMapping("/test/duplicate")
        void duplicate() {
            throw new DuplicateResourceException(
                    "Product SKU already exists"
            );
        }

        @org.springframework.web.bind.annotation.GetMapping("/test/server-error")
        void serverError() {
            throw new RuntimeException("Unexpected error");
        }

        @PostMapping("/test/products")
        void createProduct(
                @Valid @RequestBody ProductCreateRequest request
        ) {
            // No implementation needed for this validation test
        }
    }
}