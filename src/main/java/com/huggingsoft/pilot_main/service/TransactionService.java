package com.huggingsoft.pilot_main.service;

import com.hsoft.model.dto.v1.transactions.TransactionCreateRequestDTO;
import com.hsoft.model.dto.v1.transactions.TransactionDetailResponseDTO;
import com.hsoft.model.dto.v1.transactions.TransactionSummaryResponseDTO;
import com.hsoft.model.dto.v1.transactions.TransactionUpdateRequestDTO;
import com.hsoft.model.types.v1.TransactionStatus;
import com.hsoft.model.types.v1.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.UUID;

// --- Transaction Service ---
public interface TransactionService {
    TransactionDetailResponseDTO createSale(Object principal, TransactionCreateRequestDTO request);
    TransactionDetailResponseDTO createPurchase(Object principal, TransactionCreateRequestDTO request);
    Page<TransactionSummaryResponseDTO> findTransactions(Object principal, TransactionType type, TransactionStatus status,
                                                         UUID clientId, UUID providerId, OffsetDateTime dateFrom,
                                                         OffsetDateTime dateTo, String referenceNumber, Pageable pageable);
    TransactionDetailResponseDTO findTransactionById(Object principal, UUID transactionId);
    TransactionDetailResponseDTO updateTransaction(Object principal, UUID transactionId, TransactionUpdateRequestDTO request);
    TransactionDetailResponseDTO cancelTransaction(Object principal, UUID transactionId /*, reason? */);
}
