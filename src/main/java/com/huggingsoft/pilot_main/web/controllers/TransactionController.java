package com.huggingsoft.pilot_main.web.controllers;

import com.hsoft.model.dto.v1.transactions.TransactionCreateRequestDTO;
import com.hsoft.model.dto.v1.transactions.TransactionDetailResponseDTO;
import com.hsoft.model.dto.v1.transactions.TransactionSummaryResponseDTO;
import com.hsoft.model.dto.v1.transactions.TransactionUpdateRequestDTO;
import com.hsoft.model.types.v1.TransactionStatus;
import com.hsoft.model.types.v1.TransactionType;
import com.huggingsoft.pilot_main.service.TransactionService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "APIs for managing sales and purchase transactions")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    // Placeholder for getting the authenticated user's principal
    private Object getCurrentUserPrincipal() {
        return new Object(); // Dummy object
    }

    @PostMapping("/sales")
    @Operation(summary = "Create a new Sale transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sale transaction created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionDetailResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., missing client, negative stock)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Client or Product not found", content = @Content)
    })
    public ResponseEntity<TransactionDetailResponseDTO> createSaleTransaction(@Valid @RequestBody TransactionCreateRequestDTO createRequest) {
        TransactionDetailResponseDTO createdTransaction = transactionService.createSale(getCurrentUserPrincipal(), createRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
    }

    @PostMapping("/purchases")
    @Operation(summary = "Create a new Purchase transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Purchase transaction created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionDetailResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., missing provider)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Provider or Product not found", content = @Content)
    })
    public ResponseEntity<TransactionDetailResponseDTO> createPurchaseTransaction(@Valid @RequestBody TransactionCreateRequestDTO createRequest) {
        TransactionDetailResponseDTO createdTransaction = transactionService.createPurchase(getCurrentUserPrincipal(), createRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
    }

    @GetMapping
    @Operation(summary = "List transactions for the current user's main account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully") // Schema for Page<TransactionSummaryResponse> omitted
    })
    @Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = "integer", defaultValue = "0"))
    @Parameter(in = ParameterIn.QUERY, name = "size", description = "Page size", schema = @Schema(type = "integer", defaultValue = "20"))
    @Parameter(in = ParameterIn.QUERY, name = "sort", description = "Sort criteria (e.g., 'transactionDate,desc')", schema = @Schema(type = "string"))
    @Parameter(in = ParameterIn.QUERY, name = "type", description = "Filter by transaction type (SALE, PURCHASE)", schema = @Schema(implementation = TransactionType.class))
    @Parameter(in = ParameterIn.QUERY, name = "status", description = "Filter by status", schema = @Schema(implementation = TransactionStatus.class))
    @Parameter(in = ParameterIn.QUERY, name = "clientId", description = "Filter by client ID", schema = @Schema(type = "string", format = "uuid"))
    @Parameter(in = ParameterIn.QUERY, name = "providerId", description = "Filter by provider ID", schema = @Schema(type = "string", format = "uuid"))
    @Parameter(in = ParameterIn.QUERY, name = "dateFrom", description = "Filter transactions from this date (ISO 8601 format)", schema = @Schema(type = "string", format = "date-time"))
    @Parameter(in = ParameterIn.QUERY, name = "dateTo", description = "Filter transactions up to this date (ISO 8601 format)", schema = @Schema(type = "string", format = "date-time"))
    @Parameter(in = ParameterIn.QUERY, name = "referenceNumber", description = "Search by reference number", schema = @Schema(type = "string"))
    public ResponseEntity<Page<TransactionSummaryResponseDTO>> listTransactions(
            @Parameter(hidden = true) Pageable pageable,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) UUID providerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(required = false) String referenceNumber) {
        Page<TransactionSummaryResponseDTO> transactions = transactionService.findTransactions(
                getCurrentUserPrincipal(), type, status, clientId, providerId, dateFrom, dateTo, referenceNumber, pageable);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get details of a specific transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionDetailResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transaction not found or doesn't belong to user", content = @Content)
    })
    public ResponseEntity<TransactionDetailResponseDTO> getTransactionById(@PathVariable UUID transactionId) {
        TransactionDetailResponseDTO transaction = transactionService.findTransactionById(getCurrentUserPrincipal(), transactionId);
        return ResponseEntity.ok(transaction);
    }

    @PatchMapping("/{transactionId}")
    @Operation(summary = "Update limited details of a specific transaction (e.g., status, notes, context)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionDetailResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data or update not allowed", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content)
    })
    public ResponseEntity<TransactionDetailResponseDTO> updateTransaction(@PathVariable UUID transactionId, @Valid @RequestBody TransactionUpdateRequestDTO updateRequest) {
        TransactionDetailResponseDTO updatedTransaction = transactionService.updateTransaction(getCurrentUserPrincipal(), transactionId, updateRequest);
        return ResponseEntity.ok(updatedTransaction);
    }

    @PostMapping("/{transactionId}/cancel")
    @Operation(summary = "Cancel a specific transaction (sets status to CANCELLED)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction cancelled successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionDetailResponseDTO.class))), // Return updated transaction
            @ApiResponse(responseCode = "400", description = "Transaction cannot be cancelled (e.g., already cancelled)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content)
    })
    public ResponseEntity<TransactionDetailResponseDTO> cancelTransaction(@PathVariable UUID transactionId /*, Optional: @RequestBody CancelReason reason */) {
        TransactionDetailResponseDTO cancelledTransaction = transactionService.cancelTransaction(getCurrentUserPrincipal(), transactionId /*, reason */);
        return ResponseEntity.ok(cancelledTransaction);
    }

}
