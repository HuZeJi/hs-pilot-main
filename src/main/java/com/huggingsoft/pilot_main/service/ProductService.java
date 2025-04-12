package com.huggingsoft.pilot_main.service;

import com.hsoft.model.dto.v1.products.ProductCreateRequestDTO;
import com.hsoft.model.dto.v1.products.ProductResponseDTO;
import com.hsoft.model.dto.v1.products.ProductStockResponseDTO;
import com.hsoft.model.dto.v1.products.ProductUpdateRequestDTO;
import com.hsoft.model.dto.v1.products.StockAdjustmentRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

// --- Product Service ---
public interface ProductService {
    ProductResponseDTO createProduct(Object principal, ProductCreateRequestDTO request);
    Page<ProductResponseDTO> findProducts(Object principal, String category, Boolean isActive, String search, Pageable pageable);
    ProductResponseDTO findProductById(Object principal, UUID productId);
    ProductResponseDTO updateProduct(Object principal, UUID productId, ProductUpdateRequestDTO request);
    void updateProductStatus(Object principal, UUID productId, boolean isActive);
    void deleteProduct(Object principal, UUID productId);
    ProductStockResponseDTO adjustStock(Object principal, UUID productId, StockAdjustmentRequestDTO request);
    List<ProductStockResponseDTO> findStockLevelsByIds(Object principal, List<UUID> ids);
}
