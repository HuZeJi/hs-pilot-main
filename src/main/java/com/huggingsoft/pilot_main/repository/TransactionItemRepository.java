package com.huggingsoft.pilot_main.repository;

import com.hsoft.model.entities.v1.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link TransactionItem} entity.
 * Provides standard CRUD operations. Complex queries are typically performed
 * via the parent Transaction entity or through Specifications on TransactionRepository.
 */
@Repository
public interface TransactionItemRepository extends JpaRepository<TransactionItem, UUID> {

    /**
     * Finds all items belonging to a specific transaction.
     * Useful if needing items separate from the full transaction object.
     *
     * @param transactionId The ID of the parent transaction.
     * @return A List of transaction items.
     */
    List<TransactionItem> findByTransactionTransactionId(UUID transactionId);

    /**
     * Counts how many transaction items reference a specific product.
     * Useful for checking if a product can be deleted.
     *
     * @param productId The ID of the product.
     * @return The number of transaction items referencing the product.
     */
    long countByProductProductId(UUID productId);

}
