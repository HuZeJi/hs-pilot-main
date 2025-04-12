package com.huggingsoft.pilot_main.repository;

import com.hsoft.model.entities.v1.Product;
import com.hsoft.model.entities.v1.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Product} entity.
 * Provides standard CRUD operations and uses JpaSpecificationExecutor
 * for dynamic filtering and searching of products (e.g., by category, status, name/SKU).
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    /**
     * Checks if a product exists for a given user with the specified SKU, ignoring case.
     * Used to enforce SKU uniqueness per user before saving.
     *
     * @param user The owning user account.
     * @param sku  The SKU to check.
     * @return true if a product with the SKU exists for the user, false otherwise.
     */
    boolean existsByUserAndSkuIgnoreCase(User user, String sku);

    /**
     * Finds all products belonging to a specific user account.
     * Filtering (category, status, search) should be applied using Specifications.
     *
     * @param user     The owning user account.
     * @param pageable Pagination information.
     * @return A Page of products belonging to the user.
     */
    Page<Product> findByUser(User user, Pageable pageable);

    /**
     * Finds products by their IDs, restricted to the specified user.
     * Useful for fetching details for specific products (e.g., for stock level checks).
     *
     * @param user       The owning user account.
     * @param productIds List of Product UUIDs to find.
     * @return A List of products matching the IDs and belonging to the user.
     */
    List<Product> findByUserAndProductIdIn(User user, List<UUID> productIds);

}
