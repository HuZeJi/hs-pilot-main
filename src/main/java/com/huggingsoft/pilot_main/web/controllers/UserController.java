package com.huggingsoft.pilot_main.web.controllers;

import com.hsoft.model.dto.v1.commons.StatusUpdateRequestDTO;
import com.hsoft.model.dto.v1.users.ChangePasswordRequestDTO;
import com.hsoft.model.dto.v1.users.CompanyInfoUpdateRequestDTO;
import com.hsoft.model.dto.v1.users.SubUserCreateRequestDTO;
import com.hsoft.model.dto.v1.users.UserResponseDTO;
import com.hsoft.model.dto.v1.users.UserUpdateRequestDTO;
import com.huggingsoft.pilot_main.service.UserService;
import com.huggingsoft.pilot_main.service.utils.JwtService;
import com.huggingsoft.pilot_main.shared.RequestContext;
import com.huggingsoft.pilot_main.shared.RequestContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// Import your way of getting the Principal (e.g., Spring Security)
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "APIs for managing user profiles, sub-accounts, and company information")
@SecurityRequirement(name = "bearerAuth") // Apply security to all endpoints in this controller
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    // Placeholder for getting the authenticated user's principal
    private String getCurrentUserPrincipal() {
        // Replace with actual implementation, e.g.:
//         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//         if (authentication == null || !authentication.isAuthenticated()) return null;
//         return authentication.getPrincipal();
//        return new Object(); // Dummy object
        return "";
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user's profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserResponseDTO> getCurrentUserProfile() {

//        System.out.println(getCurrentUserPrincipal());
//        System.out.println("=====================");
//        System.out.println(authHeader);
//        // Extract token from the header
//        String token = authHeader.replace("Bearer ", "");
//        // Validate and extract user details from the token
//        String username = jwtService.extractUsername(token);
        RequestContext context = RequestContextHolder.getContext();
        System.out.println(context);
        UserResponseDTO user = userService.getCurrentUser(getCurrentUserPrincipal());
        System.out.println(user);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/me")
    @Operation(summary = "Update the current authenticated user's profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserResponseDTO> updateCurrentUserProfile(@Valid @RequestBody UserUpdateRequestDTO updateRequest) {
        UserResponseDTO updatedUser = userService.updateCurrentUser(getCurrentUserPrincipal(), updateRequest);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Change the current authenticated user's password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or current password incorrect", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<Void> changeCurrentUserPassword(@Valid @RequestBody ChangePasswordRequestDTO passwordRequest) {
        userService.changeCurrentUserPassword(getCurrentUserPrincipal(), passwordRequest);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/company")
    @Operation(summary = "Update main account company information (Main User Only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Company info updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden (not a main user)", content = @Content)
    })
    public ResponseEntity<UserResponseDTO> updateCompanyInfo(@Valid @RequestBody CompanyInfoUpdateRequestDTO updateRequest) {
        // Service layer must verify user is a main user
        UserResponseDTO updatedUser = userService.updateCompanyInfo(getCurrentUserPrincipal(), updateRequest);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete the current authenticated main user account (Main User Only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden (not a main user or cannot delete)", content = @Content)
            // Consider 400 if confirmation (e.g., password) is required and missing/invalid
    })
    public ResponseEntity<Void> deleteMainAccount(/* Optional: Add confirmation param/body */) {
        // Service layer must verify user is a main user and handle deletion logic
        userService.deleteMainAccount(getCurrentUserPrincipal() /*, confirmationData */);
        return ResponseEntity.noContent().build();
    }


    // --- Sub-User Management (Main User Only) ---

    @PostMapping("/me/sub-users")
    @Operation(summary = "Create a new sub-user account (Main User Only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sub-user created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden (not a main user)", content = @Content),
            @ApiResponse(responseCode = "409", description = "Sub-user username or email already exists", content = @Content)
    })
    public ResponseEntity<UserResponseDTO> createSubUser(@Valid @RequestBody SubUserCreateRequestDTO createRequest) {
        // Service layer must verify current user is main user
        UserResponseDTO createdSubUser = userService.createSubUser(getCurrentUserPrincipal(), createRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSubUser);
    }

    @GetMapping("/me/sub-users")
    @Operation(summary = "List sub-user accounts for the current main user (Main User Only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sub-users retrieved successfully"), // Schema for Page<UserResponseDTO> omitted for brevity
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden (not a main user)", content = @Content)
    })
    @Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number (0-indexed)", schema = @Schema(type = "integer", defaultValue = "0"))
    @Parameter(in = ParameterIn.QUERY, name = "size", description = "Number of items per page", schema = @Schema(type = "integer", defaultValue = "20"))
    @Parameter(in = ParameterIn.QUERY, name = "sort", description = "Sort criteria (e.g., 'username,asc')", schema = @Schema(type = "string"))
    @Parameter(in = ParameterIn.QUERY, name = "isActive", description = "Filter by active status", schema = @Schema(type = "boolean"))
    public ResponseEntity<Page<UserResponseDTO>> listSubUsers(
            @Parameter(hidden = true) Pageable pageable, // Spring automatically populates from query params
            @RequestParam(required = false) Boolean isActive) {
        Page<UserResponseDTO> subUsers = userService.findSubUsers(getCurrentUserPrincipal(), isActive, pageable);
        return ResponseEntity.ok(subUsers);
    }

    @GetMapping("/me/sub-users/{subUserId}")
    @Operation(summary = "Get details of a specific sub-user (Main User Only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sub-user found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden (not main user or sub-user doesn't belong)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sub-user not found", content = @Content)
    })
    public ResponseEntity<UserResponseDTO> getSubUserDetails(@PathVariable UUID subUserId) {
        UserResponseDTO subUser = userService.getSubUserById(getCurrentUserPrincipal(), subUserId);
        return ResponseEntity.ok(subUser);
    }

    @PatchMapping("/me/sub-users/{subUserId}")
    @Operation(summary = "Update details of a specific sub-user (Main User Only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sub-user updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sub-user not found", content = @Content)
    })
    public ResponseEntity<UserResponseDTO> updateSubUser(@PathVariable UUID subUserId, @Valid @RequestBody UserUpdateRequestDTO updateRequest) {
        UserResponseDTO updatedSubUser = userService.updateSubUser(getCurrentUserPrincipal(), subUserId, updateRequest);
        return ResponseEntity.ok(updatedSubUser);
    }

    @PatchMapping("/me/sub-users/{subUserId}/status")
    @Operation(summary = "Activate or deactivate a specific sub-user (Main User Only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Sub-user status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sub-user not found", content = @Content)
    })
    public ResponseEntity<Void> updateSubUserStatus(@PathVariable UUID subUserId, @Valid @RequestBody StatusUpdateRequestDTO statusRequest) {
        userService.updateSubUserStatus(getCurrentUserPrincipal(), subUserId, statusRequest.getIsActive());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/sub-users/{subUserId}")
    @Operation(summary = "Delete a specific sub-user (Main User Only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Sub-user deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sub-user not found", content = @Content)
    })
    public ResponseEntity<Void> deleteSubUser(@PathVariable UUID subUserId) {
        userService.deleteSubUser(getCurrentUserPrincipal(), subUserId);
        return ResponseEntity.noContent().build();
    }
}