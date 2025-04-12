package com.huggingsoft.pilot_main.web.controllers;

import com.hsoft.model.dto.v1.commons.StatusUpdateRequestDTO;
import com.hsoft.model.dto.v1.products.ProductCreateRequestDTO;
import com.hsoft.model.dto.v1.products.ProductResponseDTO;
import com.hsoft.model.dto.v1.products.ProductStockResponseDTO;
import com.hsoft.model.dto.v1.products.ProductUpdateRequestDTO;
import com.hsoft.model.dto.v1.products.StockAdjustmentRequestDTO;
import com.huggingsoft.pilot_main.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List; // For potential bulk stock query


@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "APIs for managing products and inventory")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    // Placeholder for getting the authenticated user's principal
    private Object getCurrentUserPrincipal() {
        // Replace with actual implementation
        return new Object(); // Dummy object
    }


    @PostMapping
    @Operation(summary = "Create a new product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "409", description = "SKU already exists for this user", content = @Content)
    })
    public ResponseEntity<ProductResponseDTO> createProduct(@Valid @RequestBody ProductCreateRequestDTO createRequest) {
        ProductResponseDTO createdProduct = productService.createProduct(getCurrentUserPrincipal(), createRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @GetMapping
    @Operation(summary = "List products for the current user's main account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully") // Schema for Page<ProductResponse> omitted
    })
    @Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = "integer", defaultValue = "0"))
    @Parameter(in = ParameterIn.QUERY, name = "size", description = "Page size", schema = @Schema(type = "integer", defaultValue = "20"))
    @Parameter(in = ParameterIn.QUERY, name = "sort", description = "Sort criteria (e.g., 'name,asc')", schema = @Schema(type = "string"))
    @Parameter(in = ParameterIn.QUERY, name = "category", description = "Filter by category", schema = @Schema(type = "string"))
    @Parameter(in = ParameterIn.QUERY, name = "isActive", description = "Filter by active status", schema = @Schema(type = "boolean"))
    @Parameter(in = ParameterIn.QUERY, name = "search", description = "Search term (name, SKU)", schema = @Schema(type = "string"))
    public ResponseEntity<Page<ProductResponseDTO>> listProducts(
            @Parameter(hidden = true) Pageable pageable,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search) {
        Page<ProductResponseDTO> products = productService.findProducts(getCurrentUserPrincipal(), category, isActive, search, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get details of a specific product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found or doesn't belong to user", content = @Content)
    })
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable UUID productId) {
        ProductResponseDTO product = productService.findProductById(getCurrentUserPrincipal(), productId);
        return ResponseEntity.ok(product);
    }

    @PatchMapping("/{productId}")
    @Operation(summary = "Update details of a specific product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Updated SKU already exists", content = @Content)
    })
    public ResponseEntity<ProductResponseDTO> updateProduct(@PathVariable UUID productId, @Valid @RequestBody ProductUpdateRequestDTO updateRequest) {
        ProductResponseDTO updatedProduct = productService.updateProduct(getCurrentUserPrincipal(), productId, updateRequest);
        return ResponseEntity.ok(updatedProduct);
    }

    @PatchMapping("/{productId}/status")
    @Operation(summary = "Activate or deactivate a specific product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<Void> updateProductStatus(@PathVariable UUID productId, @Valid @RequestBody StatusUpdateRequestDTO statusRequest) {
        productService.updateProductStatus(getCurrentUserPrincipal(), productId, statusRequest.getIsActive());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete a specific product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflict (e.g., product used in transactions)", content = @Content)
    })
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID productId) {
        productService.deleteProduct(getCurrentUserPrincipal(), productId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productId}/stock")
    @Operation(summary = "Manually adjust the stock of a specific product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock adjusted successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductStockResponseDTO.class))), // Or ProductResponse
            @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., negative stock not allowed)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<ProductStockResponseDTO> adjustStock(
            @PathVariable UUID productId,
            @Valid @RequestBody StockAdjustmentRequestDTO adjustmentRequest) {
        ProductStockResponseDTO updatedStock = productService.adjustStock(getCurrentUserPrincipal(), productId, adjustmentRequest);
        return ResponseEntity.ok(updatedStock);
    }

    // Optional: Endpoint for getting just stock for multiple products
    @GetMapping("/stock")
    @Operation(summary = "Get current stock levels for multiple products")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock levels retrieved")
    })
    public ResponseEntity<List<ProductStockResponseDTO>> getMultipleStockLevels(
            @RequestParam List<UUID> ids) {
        List<ProductStockResponseDTO> stockLevels = productService.findStockLevelsByIds(getCurrentUserPrincipal(), ids);
        return ResponseEntity.ok(stockLevels);
    }
}
