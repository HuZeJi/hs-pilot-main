package com.huggingsoft.pilot_main.repository;

import com.hsoft.model.entities.v1.PasswordResetToken;
import com.hsoft.model.entities.v1.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // For delete methods

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link PasswordResetToken} entity.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Finds a password reset token by its unique token string.
     * This is the primary method used during the password reset confirmation step.
     *
     * @param token The token string to search for.
     * @return An Optional containing the token entity if found, otherwise empty.
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Finds the most recent (or potentially only) token associated with a specific user.
     * Useful if enforcing a "one active token per user" rule.
     *
     * @param user The user account.
     * @return An Optional containing the token entity if found.
     */
    Optional<PasswordResetToken> findByUser(User user); // Consider ordering by expiryDate DESC if multiple allowed

    /**
     * Deletes all password reset tokens associated with a specific user.
     * Can be used when a user successfully resets their password or if the user account is deleted.
     *
     * @param user The user whose tokens should be deleted.
     */
    @Transactional // Required for delete operations derived from method name
    void deleteByUser(User user);

    /**
     * Deletes all password reset tokens whose expiry date is before the specified timestamp.
     * Useful for implementing a scheduled cleanup task for expired tokens.
     *
     * @param now The timestamp indicating the cutoff for expired tokens.
     * @return The number of tokens deleted.
     */
    @Transactional // Required for delete operations derived from method name
    long deleteByExpiryDateBefore(OffsetDateTime now);

}
