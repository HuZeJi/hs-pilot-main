package com.huggingsoft.pilot_main.service.impl;

import com.hsoft.model.dto.v1.commons.ClientSummaryResponseDTO;
import com.hsoft.model.dto.v1.commons.ProductSummaryResponseDTO;
import com.hsoft.model.dto.v1.commons.ProviderSummaryResponseDTO;
import com.hsoft.model.dto.v1.commons.UserSummaryResponseDTO;
import com.hsoft.model.dto.v1.transactions.TransactionCreateRequestDTO;
import com.hsoft.model.dto.v1.transactions.TransactionDetailResponseDTO;
import com.hsoft.model.dto.v1.transactions.TransactionItemCreateRequestDTO;
import com.hsoft.model.dto.v1.transactions.TransactionItemResponseDTO;
import com.hsoft.model.dto.v1.transactions.TransactionSummaryResponseDTO;
import com.hsoft.model.dto.v1.transactions.TransactionUpdateRequestDTO;
import com.hsoft.model.entities.v1.Client;
import com.hsoft.model.entities.v1.Product;
import com.hsoft.model.entities.v1.Provider;
import com.hsoft.model.entities.v1.Transaction;
import com.hsoft.model.entities.v1.TransactionItem;
import com.hsoft.model.entities.v1.User;
import com.hsoft.model.mappers.ClientMapper;
import com.hsoft.model.mappers.ProductMapper;
import com.hsoft.model.mappers.ProviderMapper;
import com.hsoft.model.mappers.TransactionItemMapper;
import com.hsoft.model.mappers.TransactionMapper;
import com.hsoft.model.mappers.UserMapper;
import com.hsoft.model.types.v1.TransactionStatus;
import com.hsoft.model.types.v1.TransactionType;
import com.huggingsoft.pilot_main.repository.ClientRepository;
import com.huggingsoft.pilot_main.repository.ProductRepository;
import com.huggingsoft.pilot_main.repository.ProviderRepository;
import com.huggingsoft.pilot_main.repository.TransactionRepository;
import com.huggingsoft.pilot_main.repository.UserRepository;
import com.huggingsoft.pilot_main.service.TransactionService;
import com.huggingsoft.pilot_main.service.exceptions.BusinessRuleViolationException;
import com.huggingsoft.pilot_main.service.exceptions.ResourceNotFoundException;
import com.huggingsoft.pilot_main.service.exceptions.UnauthorizedOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// --- Transaction Service Implementation ---
@Service
@RequiredArgsConstructor
// Default Transactional (readOnly=false) needed as most methods modify data (incl. stock)
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final ProviderRepository providerRepository;
    // No TransactionItemRepository needed directly if using cascade persist
    private final TransactionMapper transactionMapper;
    private final TransactionItemMapper transactionItemMapper;
    private final UserMapper userMapper;
    private final ClientMapper clientMapper;
    private final ProviderMapper providerMapper;
    private final ProductMapper productMapper;

    // Helper method to get main user
    private User getMainUserFromPrincipal(Object principal) { /* ... */ return null;}
    // Helper method to get the actual user performing the action (main or sub)
    private User getCreatorUserFromPrincipal(Object principal) { /* ... */ return null;}


    @Override
    public TransactionDetailResponseDTO createSale(Object principal, TransactionCreateRequestDTO request) {
        User mainUser = getMainUserFromPrincipal(principal);
        User creator = getCreatorUserFromPrincipal(principal);

        if (request.getClientId() == null) {
            throw new BusinessRuleViolationException("Client ID is required for SALE transactions.");
        }
        // Fetch client and verify ownership
        Client client = clientRepository.findById(request.getClientId())
                .filter(c -> c.getUser().getUserId().equals(mainUser.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Client not found or does not belong to user: " + request.getClientId()));

        Transaction transaction = mapToTransactionEntity(request, mainUser, creator);
        transaction.setTransactionType(TransactionType.SALE);
        transaction.setClient(client);
        transaction.setProvider(null); // Ensure provider is null for sale

        processTransactionItems(transaction, request.getItems(), mainUser, true); // true for sale (decrease stock)
        calculateTransactionTotal(transaction);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.transactionToTransactionDetailResponseDTO(savedTransaction);
    }

    @Override
    public TransactionDetailResponseDTO createPurchase(Object principal, TransactionCreateRequestDTO request) {
        User mainUser = getMainUserFromPrincipal(principal);
        User creator = getCreatorUserFromPrincipal(principal);

        if (request.getProviderId() == null) {
            throw new BusinessRuleViolationException("Provider ID is required for PURCHASE transactions.");
        }
        // Fetch provider and verify ownership
        Provider provider = providerRepository.findById(request.getProviderId())
                .filter(p -> p.getUser().getUserId().equals(mainUser.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found or does not belong to user: " + request.getProviderId()));

        Transaction transaction = mapToTransactionEntity(request, mainUser, creator);
        transaction.setTransactionType(TransactionType.PURCHASE);
        transaction.setProvider(provider);
        transaction.setClient(null); // Ensure client is null for purchase

        processTransactionItems(transaction, request.getItems(), mainUser, false); // false for purchase (increase stock)
        calculateTransactionTotal(transaction);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.transactionToTransactionDetailResponseDTO(savedTransaction);
    }


    // --- Private helpers for create ---
    private void processTransactionItems(Transaction transaction, List<TransactionItemCreateRequestDTO> itemRequests, User mainUser, boolean isSale) {
        List<TransactionItem> items = new ArrayList<>();
        for (TransactionItemCreateRequestDTO itemDto : itemRequests) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .filter(p -> p.getUser().getUserId().equals(mainUser.getUserId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found or does not belong to user: " + itemDto.getProductId()));

            // Create Transaction Item
            TransactionItem item = new TransactionItem();
            item.setTransaction(transaction); // Link back to parent
            item.setProduct(product);
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(itemDto.getUnitPrice());
            item.setContext(itemDto.getContext() != null ? itemDto.getContext() : new HashMap<>());
            item.calculateSubtotal(); // Ensure subtotal is calculated

            items.add(item);

            // --- Stock Adjustment ---
            int currentStock = product.getCurrentStock();
            int quantityChange = itemDto.getQuantity();
            int newStock;

            if (isSale) {
                newStock = currentStock - quantityChange;
                if (newStock < 0) {
                    // Optional: Allow negative stock? Configurable?
                    throw new BusinessRuleViolationException("Insufficient stock for product: " + product.getName() + " (ID: " + product.getProductId() + "). Required: " + quantityChange + ", Available: " + currentStock);
                }
            } else { // Purchase
                newStock = currentStock + quantityChange;
            }
            product.setCurrentStock(newStock);
            // No need to call productRepository.save() here if using CascadeType.PERSIST/MERGE on Transaction.items
            // and Transaction entity is saved later. If not using cascade, save product here.
            // productRepository.save(product); // Uncomment if not using cascade from Transaction
        }
        transaction.setItems(items); // Set the processed items on the transaction
    }

    private void calculateTransactionTotal(Transaction transaction) {
        BigDecimal total = transaction.getItems().stream()
                .map(TransactionItem::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        transaction.setTotalAmount(total);
    }

    // --- Find Methods ---
    @Override
    @Transactional(readOnly=true)
    public Page<TransactionSummaryResponseDTO> findTransactions(Object principal, TransactionType type, TransactionStatus status, UUID clientId, UUID providerId, OffsetDateTime dateFrom, OffsetDateTime dateTo, String referenceNumber, Pageable pageable) {
        User mainUser = getMainUserFromPrincipal(principal);
        Specification<Transaction> spec = buildTransactionSpecification(mainUser, type, status, clientId, providerId, dateFrom, dateTo, referenceNumber);
        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageable);
        // Map to Summary DTO - Requires fetching client/provider names potentially
        return transactionPage.map(transactionMapper::transactionToTransactionSummaryResponseDTO); // Implement mapping
    }

    private Specification<Transaction> buildTransactionSpecification(User mainUser, TransactionType type, TransactionStatus status, UUID clientId, UUID providerId, OffsetDateTime dateFrom, OffsetDateTime dateTo, String referenceNumber) {
        Specification<Transaction> spec = Specification.where((root, query, cb) -> cb.equal(root.get("user"), mainUser));

        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("transactionType"), type));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (clientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("client").get("clientId"), clientId));
        }
        if (providerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("provider").get("providerId"), providerId));
        }
        if (dateFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), dateFrom));
        }
        if (dateTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), dateTo));
        }
        if (StringUtils.hasText(referenceNumber)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("referenceNumber")), "%" + referenceNumber.toLowerCase() + "%"));
        }
        return spec;
    }


    @Override
    @Transactional(readOnly=true) // Ensure graph fetch happens in transaction
    public TransactionDetailResponseDTO findTransactionById(Object principal, UUID transactionId) {
        User mainUser = getMainUserFromPrincipal(principal);
        Transaction transaction = transactionRepository.findDetailedByTransactionId(transactionId) // Use EntityGraph fetch
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));

        // Verify ownership
        if (!transaction.getUser().getUserId().equals(mainUser.getUserId())) {
            throw new UnauthorizedOperationException("Transaction does not belong to the authenticated user.");
        }
        return transactionMapper.transactionToTransactionDetailResponseDTO(transaction);
    }

    // --- Update/Cancel ---
    @Override
    public TransactionDetailResponseDTO updateTransaction(Object principal, UUID transactionId, TransactionUpdateRequestDTO request) {
        User mainUser = getMainUserFromPrincipal(principal);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));

        if (!transaction.getUser().getUserId().equals(mainUser.getUserId())) {
            throw new UnauthorizedOperationException("Transaction does not belong to the authenticated user.");
        }

        // Update allowed fields
        if (request.getStatus() != null) transaction.setStatus(request.getStatus());
        if (request.getNotes() != null) transaction.setNotes(request.getNotes());
        if (request.getReferenceNumber() != null) transaction.setReferenceNumber(request.getReferenceNumber());
        if (request.getContext() != null) transaction.getContext().putAll(request.getContext());

        // IMPORTANT: Do NOT allow changing items, client, provider, totalAmount etc. here.
        //            Use specific actions like cancelTransaction or create adjustments.

        Transaction updatedTransaction = transactionRepository.save(transaction);
        // Re-fetch with graph or map carefully if needed for response
        return transactionMapper.transactionToTransactionDetailResponseDTO(updatedTransaction);
    }

    @Override
    public TransactionDetailResponseDTO cancelTransaction(Object principal, UUID transactionId) {
        User mainUser = getMainUserFromPrincipal(principal);
        // Fetch with items needed to revert stock
        Transaction transaction = transactionRepository.findDetailedByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));

        if (!transaction.getUser().getUserId().equals(mainUser.getUserId())) {
            throw new UnauthorizedOperationException("Transaction does not belong to the authenticated user.");
        }
        if (transaction.getStatus() == TransactionStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Transaction is already cancelled.");
        }

        // Revert Stock Changes
        boolean isSale = transaction.getTransactionType() == TransactionType.SALE;
        for(TransactionItem item : transaction.getItems()) {
            Product product = item.getProduct(); // Assumes product is loaded by EntityGraph
            if (product != null) {
                int quantityChange = item.getQuantity();
                int currentStock = product.getCurrentStock();
                int originalStock;
                if (isSale) { // Revert sale: add stock back
                    originalStock = currentStock + quantityChange;
                } else { // Revert purchase: remove stock
                    originalStock = currentStock - quantityChange;
                    // Check if reverting purchase makes stock negative? Usually allowed.
                }
                product.setCurrentStock(originalStock);
                // productRepository.save(product); // Save if not using cascade merge/persist
            }
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        // Add cancellation reason/notes if provided
        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.transactionToTransactionDetailResponseDTO(savedTransaction);
    }


    // --- Mappers ---
    private Transaction mapToTransactionEntity(TransactionCreateRequestDTO dto, User mainUser, User creator) {
        Transaction tx = new Transaction();
        tx.setUser(mainUser);
        tx.setCreatedByUser(creator);
        tx.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : OffsetDateTime.now());
        tx.setReferenceNumber(dto.getReferenceNumber());
        tx.setNotes(dto.getNotes());
        tx.setStatus(dto.getStatus() != null ? dto.getStatus() : TransactionStatus.COMPLETED);
        tx.setContext(dto.getContext() != null ? dto.getContext() : new HashMap<>());
        // Client/Provider set based on type
        // Items processed separately
        // TotalAmount calculated separately
        return tx;
    }
//
//    private TransactionDetailResponseDTO mapToTransactionDetailResponse(Transaction entity) { /* Implement mapping, including items */ return null;}
//    private TransactionSummaryResponseDTO mapToTransactionSummaryResponse(Transaction entity) { /* Implement mapping */ return null;}
//    private TransactionItemResponseDTO mapTransactionItemToResponse(TransactionItem item) { /* Implement mapping */ return null;}
//    private UserSummaryResponseDTO mapToUserSummaryResponse(User user) { /* Implement mapping */ return null;}
//    private ProductSummaryResponseDTO mapToProductSummaryResponse(Product product) { /* Implement mapping */ return null;}
//    private ClientSummaryResponseDTO mapToClientSummaryResponse(Client client) { /* Implement mapping */ return null;}
//    private ProviderSummaryResponseDTO mapToProviderSummaryResponse(Provider provider) { /* Implement mapping */ return null;}


}
