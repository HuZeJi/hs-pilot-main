package com.huggingsoft.pilot_main.web.controllers;

import com.hsoft.model.dto.v1.commons.StatusUpdateRequestDTO;
import com.hsoft.model.dto.v1.providers.ProviderCreateRequestDTO;
import com.hsoft.model.dto.v1.providers.ProviderResponseDTO;
import com.hsoft.model.dto.v1.providers.ProviderUpdateRequestDTO;
import com.huggingsoft.pilot_main.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@Tag(name = "Providers", description = "APIs for managing providers (suppliers)")
@SecurityRequirement(name = "bearerAuth")
public class ProviderController {

    private final ProviderService providerService;

    // Placeholder for getting the authenticated user's principal
    private Object getCurrentUserPrincipal() {
        return new Object(); // Dummy object
    }

    @PostMapping
    @Operation(summary = "Create a new provider")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Provider created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProviderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "409", description = "Provider NIT already exists for this user", content = @Content)
    })
    public ResponseEntity<ProviderResponseDTO> createProvider(@Valid @RequestBody ProviderCreateRequestDTO createRequest) {
        ProviderResponseDTO createdProvider = providerService.createProvider(getCurrentUserPrincipal(), createRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProvider);
    }

    @GetMapping
    @Operation(summary = "List providers for the current user's main account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Providers retrieved successfully") // Schema for Page<ProviderResponse> omitted
    })
    @Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = "integer", defaultValue = "0"))
    @Parameter(in = ParameterIn.QUERY, name = "size", description = "Page size", schema = @Schema(type = "integer", defaultValue = "20"))
    @Parameter(in = ParameterIn.QUERY, name = "sort", description = "Sort criteria (e.g., 'name,asc')", schema = @Schema(type = "string"))
    @Parameter(in = ParameterIn.QUERY, name = "isActive", description = "Filter by active status", schema = @Schema(type = "boolean"))
    @Parameter(in = ParameterIn.QUERY, name = "search", description = "Search term (name, NIT, email)", schema = @Schema(type = "string"))
    public ResponseEntity<Page<ProviderResponseDTO>> listProviders(
            @Parameter(hidden = true) Pageable pageable,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search) {
        Page<ProviderResponseDTO> providers = providerService.findProviders(getCurrentUserPrincipal(), isActive, search, pageable);
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/{providerId}")
    @Operation(summary = "Get details of a specific provider")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Provider found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProviderResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Provider not found or doesn't belong to user", content = @Content)
    })
    public ResponseEntity<ProviderResponseDTO> getProviderById(@PathVariable UUID providerId) {
        ProviderResponseDTO provider = providerService.findProviderById(getCurrentUserPrincipal(), providerId);
        return ResponseEntity.ok(provider);
    }

    @PatchMapping("/{providerId}")
    @Operation(summary = "Update details of a specific provider")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Provider updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProviderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Provider not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Updated NIT already exists", content = @Content)
    })
    public ResponseEntity<ProviderResponseDTO> updateProvider(@PathVariable UUID providerId, @Valid @RequestBody ProviderUpdateRequestDTO updateRequest) {
        ProviderResponseDTO updatedProvider = providerService.updateProvider(getCurrentUserPrincipal(), providerId, updateRequest);
        return ResponseEntity.ok(updatedProvider);
    }

    @PatchMapping("/{providerId}/status")
    @Operation(summary = "Activate or deactivate a specific provider")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Provider status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Provider not found", content = @Content)
    })
    public ResponseEntity<Void> updateProviderStatus(@PathVariable UUID providerId, @Valid @RequestBody StatusUpdateRequestDTO statusRequest) {
        providerService.updateProviderStatus(getCurrentUserPrincipal(), providerId, statusRequest.getIsActive());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{providerId}")
    @Operation(summary = "Delete a specific provider")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Provider deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Provider not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflict (e.g., provider used in transactions)", content = @Content)
    })
    public ResponseEntity<Void> deleteProvider(@PathVariable UUID providerId) {
        providerService.deleteProvider(getCurrentUserPrincipal(), providerId);
        return ResponseEntity.noContent().build();
    }
}
