package com.huggingsoft.pilot_main.web.controllers;

import com.hsoft.model.dto.v1.clients.ClientCreateRequestDTO;
import com.hsoft.model.dto.v1.clients.ClientResponseDTO;
import com.hsoft.model.dto.v1.clients.ClientUpdateRequestDTO;
import com.hsoft.model.dto.v1.commons.StatusUpdateRequestDTO;
import com.huggingsoft.pilot_main.service.ClientService;
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
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "APIs for managing clients (customers)")
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final ClientService clientService;

    // Placeholder for getting the authenticated user's principal
    private Object getCurrentUserPrincipal() {
        return new Object(); // Dummy object
    }

    @PostMapping
    @Operation(summary = "Create a new client")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Client created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClientResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
            // Consider 409 if NIT needs to be unique per user
    })
    public ResponseEntity<ClientResponseDTO> createClient(@Valid @RequestBody ClientCreateRequestDTO createRequest) {
        ClientResponseDTO createdClient = clientService.createClient(getCurrentUserPrincipal(), createRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClient);
    }

    @GetMapping
    @Operation(summary = "List clients for the current user's main account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clients retrieved successfully") // Schema for Page<ClientResponse> omitted
    })
    @Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = "integer", defaultValue = "0"))
    @Parameter(in = ParameterIn.QUERY, name = "size", description = "Page size", schema = @Schema(type = "integer", defaultValue = "20"))
    @Parameter(in = ParameterIn.QUERY, name = "sort", description = "Sort criteria (e.g., 'name,asc')", schema = @Schema(type = "string"))
    @Parameter(in = ParameterIn.QUERY, name = "isActive", description = "Filter by active status", schema = @Schema(type = "boolean"))
    @Parameter(in = ParameterIn.QUERY, name = "search", description = "Search term (name, NIT, email)", schema = @Schema(type = "string"))
    public ResponseEntity<Page<ClientResponseDTO>> listClients(
            @Parameter(hidden = true) Pageable pageable,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search) {
        Page<ClientResponseDTO> clients = clientService.findClients(getCurrentUserPrincipal(), isActive, search, pageable);
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{clientId}")
    @Operation(summary = "Get details of a specific client")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Client found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClientResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Client not found or doesn't belong to user", content = @Content)
    })
    public ResponseEntity<ClientResponseDTO> getClientById(@PathVariable UUID clientId) {
        ClientResponseDTO client = clientService.findClientById(getCurrentUserPrincipal(), clientId);
        return ResponseEntity.ok(client);
    }

    @PatchMapping("/{clientId}")
    @Operation(summary = "Update details of a specific client")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Client updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClientResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Client not found", content = @Content)
    })
    public ResponseEntity<ClientResponseDTO> updateClient(@PathVariable UUID clientId, @Valid @RequestBody ClientUpdateRequestDTO updateRequest) {
        ClientResponseDTO updatedClient = clientService.updateClient(getCurrentUserPrincipal(), clientId, updateRequest);
        return ResponseEntity.ok(updatedClient);
    }

    @PatchMapping("/{clientId}/status")
    @Operation(summary = "Activate or deactivate a specific client")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Client status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Client not found", content = @Content)
    })
    public ResponseEntity<Void> updateClientStatus(@PathVariable UUID clientId, @Valid @RequestBody StatusUpdateRequestDTO statusRequest) {
        clientService.updateClientStatus(getCurrentUserPrincipal(), clientId, statusRequest.getIsActive());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{clientId}")
    @Operation(summary = "Delete a specific client")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Client deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Client not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflict (e.g., client used in transactions)", content = @Content)
    })
    public ResponseEntity<Void> deleteClient(@PathVariable UUID clientId) {
        clientService.deleteClient(getCurrentUserPrincipal(), clientId);
        return ResponseEntity.noContent().build();
    }
}
