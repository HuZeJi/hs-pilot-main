package com.huggingsoft.pilot_main.repository;

import com.hsoft.model.entities.v1.Provider;
import com.hsoft.model.entities.v1.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Provider} entity.
 * Provides standard CRUD operations and uses JpaSpecificationExecutor
 * for dynamic filtering and searching of providers (e.g., by status, name/NIT).
 */
@Repository
public interface ProviderRepository extends JpaRepository<Provider, UUID>, JpaSpecificationExecutor<Provider> {

    /**
     * Checks if a provider exists for a given user with the specified NIT.
     * Used to enforce the unique constraint on user+NIT before saving.
     * Comparison might be case-sensitive depending on NIT rules.
     *
     * @param user The owning user account.
     * @param nit  The NIT to check.
     * @return true if a provider with the NIT exists for the user, false otherwise.
     */
    boolean existsByUserAndNit(User user, String nit); // Case sensitivity might matter for NIT

    /**
     * Finds all providers belonging to a specific user account.
     * Filtering (status, search) should be applied using Specifications.
     *
     * @param user     The owning user account.
     * @param pageable Pagination information.
     * @return A Page of providers belonging to the user.
     */
    Page<Provider> findByUser(User user, Pageable pageable);
}
