package com.huggingsoft.pilot_main.repository;

import com.hsoft.model.entities.v1.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link User} entity.
 * Provides standard CRUD operations and query methods based on conventions.
 * Uses JpaSpecificationExecutor for complex dynamic queries via the Criteria API.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    /**
     * Finds a user by their username, ignoring case.
     * Used for login and checking username availability.
     *
     * @param username The username to search for.
     * @return An Optional containing the user if found, otherwise empty.
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Finds a user by their email address, ignoring case.
     * Used for login and checking email availability.
     *
     * @param email The email address to search for.
     * @return An Optional containing the user if found, otherwise empty.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Checks if a user exists with the given username, ignoring case.
     *
     * @param username The username to check.
     * @return true if a user with the username exists, false otherwise.
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Checks if a user exists with the given email address, ignoring case.
     *
     * @param email The email address to check.
     * @return true if a user with the email exists, false otherwise.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Finds all sub-users belonging to a specific parent user.
     * Note: Additional filtering (e.g., by active status) should be handled
     * using JpaSpecificationExecutor if dynamic criteria are needed.
     *
     * @param parentUser The main user account.
     * @param pageable   Pagination information.
     * @return A Page of sub-users belonging to the parent.
     */
    Page<User> findByParentUser(User parentUser, Pageable pageable);

    /**
     * Finds sub-users by parent and active status.
     * Example of a derived query. Consider using Specifications for more complex filtering.
     *
     * @param parentUser The main user account.
     * @param isActive   The desired active status.
     * @param pageable   Pagination information.
     * @return A Page of sub-users matching the criteria.
     */
    Page<User> findByParentUserAndIsActive(User parentUser, boolean isActive, Pageable pageable);
}
