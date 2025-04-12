package com.huggingsoft.pilot_main.repository;

import com.hsoft.model.entities.v1.Transaction;
import com.hsoft.model.entities.v1.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Transaction} entity.
 * Provides standard CRUD operations and uses JpaSpecificationExecutor
 * for dynamic filtering and searching of transactions (by type, status, client,
 * provider, date range, etc.).
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    /**
     * Finds all transactions belonging to a specific user account.
     * Filtering should be applied using Specifications passed to findAll methods.
     *
     * @param user     The owning user account.
     * @param pageable Pagination information.
     * @return A Page of transactions belonging to the user.
     */
    Page<Transaction> findByUser(User user, Pageable pageable);


    /**
     * Finds a transaction by its ID, potentially fetching related entities eagerly.
     * Uses an EntityGraph to define which associations to fetch to avoid N+1 problems
     * when accessing details like items, client, provider, createdByUser.
     * Define the "Transaction.detail" graph in the Transaction entity.
     *
     * @param transactionId The ID of the transaction.
     * @return An Optional containing the transaction with fetched associations if found.
     */
    @EntityGraph(value = "Transaction.detail", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Transaction> findDetailedByTransactionId(UUID transactionId);

    /**
     * Finds a transaction by its ID (standard findById).
     * Associations like items will be loaded lazily by default.
     *
     * @param transactionId The ID of the transaction.
     * @return An Optional containing the transaction if found.
     */
    @Override
    @EntityGraph(attributePaths = {"client", "provider", "createdByUser"}) // Example smaller graph
    Optional<Transaction> findById(UUID transactionId);

    // Note: Methods like countByClient or countByProvider can be derived or implemented
    // using Specifications if needed for deletion checks (checking for conflicts).
    // long countByClientId(UUID clientId);
    // long countByProviderId(UUID providerId);

}
