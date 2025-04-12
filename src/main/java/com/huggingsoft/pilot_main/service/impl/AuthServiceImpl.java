package com.huggingsoft.pilot_main.service.impl;

import com.hsoft.model.dto.v1.users.LoginRequestDTO;
import com.hsoft.model.dto.v1.users.LoginResponseDTO;
import com.hsoft.model.dto.v1.users.RegisterRequestDTO;
import com.hsoft.model.dto.v1.users.UserResponseDTO;
import com.hsoft.model.entities.v1.PasswordResetToken;
import com.hsoft.model.entities.v1.User;
import com.hsoft.model.mappers.UserMapper;
import com.huggingsoft.pilot_main.repository.PasswordResetTokenRepository;
import com.huggingsoft.pilot_main.repository.UserRepository;
import com.huggingsoft.pilot_main.service.AuthService;
import com.huggingsoft.pilot_main.service.EmailService;
import com.huggingsoft.pilot_main.service.exceptions.BusinessRuleViolationException;
import com.huggingsoft.pilot_main.service.exceptions.DataConflictException;
import com.huggingsoft.pilot_main.service.exceptions.ResourceNotFoundException;
import com.huggingsoft.pilot_main.service.exceptions.UnauthorizedOperationException;
import com.huggingsoft.pilot_main.service.utils.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder; // Import Spring Security's PasswordEncoder
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

// --- Auth Service Implementation ---
@Service
@RequiredArgsConstructor
@Transactional // Apply transactionality at the class level where appropriate
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Inject PasswordEncoder Bean
    private final JwtService jwtService; // Assuming a service to handle JWT generation/validation
    private final PasswordResetTokenRepository passwordResetTokenRepository; // Inject token repo
    private final EmailService emailService; // Inject EmailService
    private final UserMapper userMapper; // Assuming a mapper for User entity to DTO conversion

    // Inject the frontend URL property
    @Value("${app.frontend.password-reset-url}")
    private String passwordResetBaseUrl;

    @Override
    public UserResponseDTO registerMainUser(RegisterRequestDTO request) {
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new DataConflictException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DataConflictException("Email already exists: " + request.getEmail());
        }

        User newUser = userMapper.registerRequestDTOToUser(request);
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setParentUser(null); // Explicitly main user
        newUser.setActive(true); // Default active on registration

        User savedUser = userRepository.save(newUser);
        return userMapper.userToUserResponseDTO(savedUser);
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByUsernameIgnoreCase(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmailIgnoreCase(request.getUsernameOrEmail()))
                .orElseThrow(() -> new UnauthorizedOperationException("Invalid credentials"));

        if (!user.isActive()) {
            throw new UnauthorizedOperationException("User account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedOperationException("Invalid credentials");
        }

        // --- Generate JWT Token ---
         String token = jwtService.generateToken(user); // Implement this
//        String token = "dummy-jwt-token-for-" + user.getUsername(); // Placeholder

        return new LoginResponseDTO(token);
    }

    @Override
    @Transactional // Ensure token saving and potential email sending are atomic (if not async)
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 1. Generate Token (unique, secure, with expiry)
            String token = UUID.randomUUID().toString();
            // Define expiry (e.g., 1 hour from now)
            OffsetDateTime expiryDate = OffsetDateTime.now().plus(1, ChronoUnit.HOURS);

            // 2. Store Token in DB
            PasswordResetToken prt = new PasswordResetToken(token, user, expiryDate);
            // Optional: Invalidate any previous tokens for this user
            passwordResetTokenRepository.deleteByUser(user);
            passwordResetTokenRepository.save(prt);

            // 3. Construct Reset Link
            // Ensure the URL includes the token as a query parameter or path variable
            // matching what your frontend expects.
            String resetLink = passwordResetBaseUrl + "?token=" + token; // Example using query param

            // 4. Send Email using the EmailService
            try {
                emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetLink);
                log.info("Password reset email initiated for {}", email);
            } catch (Exception e) {
                // Log error, but don't reveal failure to the user initiating the request
                // to prevent user enumeration attacks. The request itself is still 'accepted'.
                log.error("Failed to send password reset email to {} during request processing: {}", email, e.getMessage());
                // If email sending MUST succeed for the operation to be considered complete,
                // re-throw a specific exception here. Otherwise, logging is often sufficient.
            }
        } else {
            log.warn("Password reset requested for non-existent email: {}", email);
            // No error thrown to the caller
        }
    }

    @Override
    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        // 1. Find token in repository and validate
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid or expired password reset token."));

        if (prt.getExpiryDate().isBefore(OffsetDateTime.now())) {
            passwordResetTokenRepository.delete(prt); // Clean up expired token
            throw new BusinessRuleViolationException("Invalid or expired password reset token.");
        }

        User user = prt.getUser();
        if (user == null) {
            // Should not happen due to DB constraints, but good practice to check
            passwordResetTokenRepository.delete(prt);
            throw new ResourceNotFoundException("User associated with the token not found.");
        }

        // 2. Update user's password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 3. Invalidate/Delete the token (and potentially others for the same user)
        passwordResetTokenRepository.deleteByUser(user); // Delete all tokens for this user

        log.info("Password successfully reset for user {}", user.getUsername());
    }
}
