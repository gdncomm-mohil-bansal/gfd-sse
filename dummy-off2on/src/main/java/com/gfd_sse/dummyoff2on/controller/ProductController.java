package com.gfd_sse.dummyoff2on.controller;

import com.gfd_sse.dummyoff2on.dto.ApiResponse;
import com.gfd_sse.dummyoff2on.model.Product;
import com.gfd_sse.dummyoff2on.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /api/products - Get all products
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts() {
        logger.info("Received request to fetch all products");
        try {
            List<Product> products = productService.getAllProducts();
            return ResponseEntity.ok(ApiResponse.success(products, "Products fetched successfully"));
        } catch (Exception e) {
            logger.error("Error fetching products", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch products: " + e.getMessage()));
        }
    }

    /**
     * GET /api/products/{id} - Get product by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        logger.info("Received request to fetch product with ID: {}", id);
        try {
            Optional<Product> product = productService.getProductById(id);
            if (product.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(product.get(), "Product fetched successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching product", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch product: " + e.getMessage()));
        }
    }

    /**
     * GET /api/products/category/{category} - Get products by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<Product>>> getProductsByCategory(@PathVariable String category) {
        logger.info("Received request to fetch products in category: {}", category);
        try {
            List<Product> products = productService.getProductsByCategory(category);
            return ResponseEntity.ok(ApiResponse.success(products,
                    "Products in category '" + category + "' fetched successfully"));
        } catch (Exception e) {
            logger.error("Error fetching products by category", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch products: " + e.getMessage()));
        }
    }

    /**
     * GET /api/products/health - Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("OK", "Product service is healthy"));
    }
}
