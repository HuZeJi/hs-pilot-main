package com.huggingsoft.pilot_main.web.controllers;

import com.hsoft.model.dto.v1.users.ConfirmPasswordResetRequestDTO;
import com.hsoft.model.dto.v1.users.LoginRequestDTO;
import com.hsoft.model.dto.v1.users.LoginResponseDTO;
import com.hsoft.model.dto.v1.users.PasswordResetRequestDTO;
import com.hsoft.model.dto.v1.users.RegisterRequestDTO;
import com.hsoft.model.dto.v1.users.UserResponseDTO;
import com.huggingsoft.pilot_main.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor // Lombok for constructor injection
@Tag(name = "Authentication", description = "APIs for user registration, login, and password management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new main user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation errors)", content = @Content),
            @ApiResponse(responseCode = "409", description = "Username or email already exists", content = @Content)
    })
    public ResponseEntity<UserResponseDTO> registerMainAccount(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        UserResponseDTO registeredUser = authService.registerMainUser(registerRequest);
        // Consider returning location header: .created(URI.create("/api/v1/users/" + registeredUser.getUserId()))
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user and return a JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        LoginResponseDTO loginResponse = authService.login(loginRequest);
        return ResponseEntity.ok(loginResponse);
    }

    // Optional: Logout endpoint if using server-side token invalidation
    // @PostMapping("/logout")
    // @Operation(summary = "Logs out the current user (if server-side invalidation is used)")
    // @SecurityRequirement(name = "bearerAuth")
    // @ApiResponse(responseCode = "204", description = "Logout successful", content = @Content)
    // @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    // public ResponseEntity<Void> logout() {
    //     // Get principal, invalidate token server-side
    //     // authService.logout(getCurrentUserPrincipal());
    //     return ResponseEntity.noContent().build();
    // }

    @PostMapping("/password-reset/request")
    @Operation(summary = "Request a password reset link via email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Password reset request accepted (email will be sent if user exists)"),
            @ApiResponse(responseCode = "400", description = "Invalid email format")
    })
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDTO request) {
        authService.requestPasswordReset(request.getEmail());
        // Always return 202 to prevent user enumeration
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "Confirm password reset using a token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or invalid/expired token")
    })
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody ConfirmPasswordResetRequestDTO request) {
        authService.confirmPasswordReset(request.getResetToken(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
