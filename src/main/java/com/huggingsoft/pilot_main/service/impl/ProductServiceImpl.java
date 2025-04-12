package com.huggingsoft.pilot_main.service.impl;

import com.hsoft.model.dto.v1.products.ProductCreateRequestDTO;
import com.hsoft.model.dto.v1.products.ProductResponseDTO;
import com.hsoft.model.dto.v1.products.ProductStockResponseDTO;
import com.hsoft.model.dto.v1.products.ProductUpdateRequestDTO;
import com.hsoft.model.dto.v1.products.StockAdjustmentRequestDTO;
import com.hsoft.model.entities.v1.Product;
import com.hsoft.model.entities.v1.User;
import com.hsoft.model.mappers.ProductMapper;
import com.huggingsoft.pilot_main.repository.ProductRepository;
import com.huggingsoft.pilot_main.repository.TransactionItemRepository;
import com.huggingsoft.pilot_main.repository.UserRepository;
import com.huggingsoft.pilot_main.service.ProductService;
import com.huggingsoft.pilot_main.service.exceptions.BusinessRuleViolationException;
import com.huggingsoft.pilot_main.service.exceptions.DataConflictException;
import com.huggingsoft.pilot_main.service.exceptions.ResourceNotFoundException;
import com.huggingsoft.pilot_main.service.exceptions.UnauthorizedOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// --- Product Service Implementation ---
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository; // Needed to get main user
    private final TransactionItemRepository transactionItemRepository; // Needed for delete check
    private final ProductMapper productMapper;

    // --- Helper Methods (Get Main User) ---
    private User getMainUserFromPrincipal(Object principal) {
        // Replace with your actual logic based on Spring Security Principal
        String username = "dummyUser"; // Placeholder
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        // If sub-users can manage products, logic changes. Assume only main users own data.
        if (user.getParentUser() != null) {
            // Alternatively, find the main user from the sub-user principal
            // user = user.getParentUser(); // If sub-users operate on behalf of main
            throw new UnauthorizedOperationException("Operation requires a main user account.");
        }
        return user;
    }

    private Product findProductForUser(User user, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        if (!product.getUser().getUserId().equals(user.getUserId())) {
            throw new UnauthorizedOperationException("Product does not belong to the authenticated user.");
        }
        return product;
    }

    // --- Public Methods ---
    @Override
    @Transactional
    public ProductResponseDTO createProduct(Object principal, ProductCreateRequestDTO request) {
        User mainUser = getMainUserFromPrincipal(principal);

        // Check SKU uniqueness for this user
        if (StringUtils.hasText(request.getSku()) && productRepository.existsByUserAndSkuIgnoreCase(mainUser, request.getSku())) {
            throw new DataConflictException("SKU already exists for this user: " + request.getSku());
        }

        Product product = productMapper.productCreateRequestDTOToProduct(request);
        product.setUser(mainUser); // Set owner

        Product savedProduct = productRepository.save(product);
        return productMapper.productToProductResponseDTO(savedProduct);
    }

    @Override
    public Page<ProductResponseDTO> findProducts(Object principal, String category, Boolean isActive, String search, Pageable pageable) {
        User mainUser = getMainUserFromPrincipal(principal);

        Specification<Product> spec = Specification.where(ProductSpecifications.belongsToUser(mainUser));
        if (StringUtils.hasText(category)) {
            spec = spec.and(ProductSpecifications.hasCategory(category));
        }
        if (isActive != null) {
            spec = spec.and(ProductSpecifications.isActive(isActive));
        }
        if (StringUtils.hasText(search)) {
            spec = spec.and(ProductSpecifications.nameOrSkuContains(search));
        }

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        return productPage.map(productMapper::productToProductResponseDTO);
    }

    @Override
    public ProductResponseDTO findProductById(Object principal, UUID productId) {
        User mainUser = getMainUserFromPrincipal(principal);
        Product product = findProductForUser(mainUser, productId); // Verifies ownership
        return productMapper.productToProductResponseDTO(product);
    }

    @Override
    @Transactional
    public ProductResponseDTO updateProduct(Object principal, UUID productId, ProductUpdateRequestDTO request) {
        User mainUser = getMainUserFromPrincipal(principal);
        Product product = findProductForUser(mainUser, productId); // Verifies ownership

        // Check SKU uniqueness if changed
        if (StringUtils.hasText(request.getSku()) && !request.getSku().equalsIgnoreCase(product.getSku())) {
            if (productRepository.existsByUserAndSkuIgnoreCase(mainUser, request.getSku())) {
                throw new DataConflictException("SKU already exists for this user: " + request.getSku());
            }
            product.setSku(request.getSku());
        }

        // Update other fields
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPurchasePrice() != null) product.setPurchasePrice(request.getPurchasePrice());
        if (request.getSalePrice() != null) product.setSalePrice(request.getSalePrice());
        if (request.getUnitOfMeasure() != null) product.setUnitOfMeasure(request.getUnitOfMeasure());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getContext() != null) {
            product.getContext().putAll(request.getContext());
        }

        Product updatedProduct = productRepository.save(product);
        return productMapper.productToProductResponseDTO(updatedProduct);
    }

    @Override
    @Transactional
    public void updateProductStatus(Object principal, UUID productId, boolean isActive) {
        User mainUser = getMainUserFromPrincipal(principal);
        Product product = findProductForUser(mainUser, productId); // Verifies ownership
        product.setActive(isActive);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Object principal, UUID productId) {
        User mainUser = getMainUserFromPrincipal(principal);
        Product product = findProductForUser(mainUser, productId); // Verifies ownership

        // Check if product is used in transactions
        long usageCount = transactionItemRepository.countByProductProductId(productId);
        if (usageCount > 0) {
            throw new DataConflictException("Cannot delete product: It is referenced in " + usageCount + " transaction(s). Consider deactivating instead.");
        }

        productRepository.delete(product);
    }

    @Override
    @Transactional
    public ProductStockResponseDTO adjustStock(Object principal, UUID productId, StockAdjustmentRequestDTO request) {
        User mainUser = getMainUserFromPrincipal(principal);
        Product product = findProductForUser(mainUser, productId); // Verifies ownership

        int currentStock = product.getCurrentStock();
        int newStock = currentStock + request.getAdjustment();

        // Optional: Prevent negative stock unless specifically allowed
        if (newStock < 0) {
            throw new BusinessRuleViolationException("Stock level cannot be negative. Current stock: " + currentStock + ", Adjustment: " + request.getAdjustment());
        }

        product.setCurrentStock(newStock);
        productRepository.save(product);

        // Log the adjustment reason? Depends on requirements.
        // log.info("Stock adjusted for product {} by {}, new stock: {}, reason: {}", productId, request.getAdjustment(), newStock, request.getReason());

        return new ProductStockResponseDTO(productId, newStock);
    }

    @Override
    public List<ProductStockResponseDTO> findStockLevelsByIds(Object principal, List<UUID> ids) {
        User mainUser = getMainUserFromPrincipal(principal);
        List<Product> products = productRepository.findByUserAndProductIdIn(mainUser, ids);
        return products.stream()
                .map(p -> new ProductStockResponseDTO(p.getProductId(), p.getCurrentStock()))
                .collect(Collectors.toList());
    }

    // --- Mappers ---
//    private Product mapToProductEntity(ProductCreateRequestDTO dto) {
//        Product product = new Product();
//        product.setSku(dto.getSku());
//        product.setName(dto.getName());
//        product.setDescription(dto.getDescription());
//        product.setPurchasePrice(dto.getPurchasePrice() != null ? dto.getPurchasePrice() : BigDecimal.ZERO);
//        product.setSalePrice(dto.getSalePrice());
//        product.setCurrentStock(dto.getCurrentStock() != null ? dto.getCurrentStock() : 0);
//        product.setUnitOfMeasure(StringUtils.hasText(dto.getUnitOfMeasure()) ? dto.getUnitOfMeasure() : "unidad");
//        product.setCategory(dto.getCategory());
//        product.setActive(dto.getIsActive() != null ? dto.getIsActive() : true);
//        product.setContext(dto.getContext() != null ? dto.getContext() : new HashMap<>());
//        return product;
//    }
//
//    private ProductResponseDTO mapToProductResponse(Product entity) {
//        if (entity == null) return null;
//        return ProductResponseDTO.builder()
//                .productId(entity.getProductId())
//                .userId(entity.getUser().getUserId())
//                .sku(entity.getSku())
//                .name(entity.getName())
//                .description(entity.getDescription())
//                .purchasePrice(entity.getPurchasePrice())
//                .salePrice(entity.getSalePrice())
//                .currentStock(entity.getCurrentStock())
//                .unitOfMeasure(entity.getUnitOfMeasure())
//                .category(entity.getCategory())
//                .isActive(entity.isActive())
//                .createdAt(entity.getCreatedAt())
//                .updatedAt(entity.getUpdatedAt())
//                .context(entity.getContext())
//                .build();
//    }

    // --- Specification Helper Class ---
    private static class ProductSpecifications {
        static Specification<Product> belongsToUser(User user) {
            return (root, query, cb) -> cb.equal(root.get("user"), user);
        }
        static Specification<Product> hasCategory(String category) {
            return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), category.toLowerCase());
        }
        static Specification<Product> isActive(boolean isActive) {
            return (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
        }
        static Specification<Product> nameOrSkuContains(String searchTerm) {
            String pattern = "%" + searchTerm.toLowerCase() + "%";
            return (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("sku")), pattern)
            );
        }
    }
}
