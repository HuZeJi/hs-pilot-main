package com.huggingsoft.pilot_main.repository;

import com.hsoft.model.entities.v1.Client;
import com.hsoft.model.entities.v1.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Client} entity.
 * Provides standard CRUD operations and uses JpaSpecificationExecutor
 * for dynamic filtering and searching of clients (e.g., by status, name/NIT).
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client> {

    /**
     * Finds all clients belonging to a specific user account.
     * Filtering (status, search) should be applied using Specifications.
     *
     * @param user     The owning user account.
     * @param pageable Pagination information.
     * @return A Page of clients belonging to the user.
     */
    Page<Client> findByUser(User user, Pageable pageable);

    // Optional: Add existsByUserAndNit if strict uniqueness (excluding C/F) needs checking here.
    // boolean existsByUserAndNitIgnoreCaseAndNitNot(User user, String nit, String cfNitValue);

}
