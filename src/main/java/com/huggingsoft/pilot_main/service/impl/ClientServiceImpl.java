package com.huggingsoft.pilot_main.service.impl;

import com.hsoft.model.dto.v1.clients.ClientCreateRequestDTO;
import com.hsoft.model.dto.v1.clients.ClientResponseDTO;
import com.hsoft.model.dto.v1.clients.ClientUpdateRequestDTO;
import com.hsoft.model.entities.v1.Client;
import com.hsoft.model.entities.v1.User;
import com.hsoft.model.mappers.ClientMapper;
import com.huggingsoft.pilot_main.repository.ClientRepository;
import com.huggingsoft.pilot_main.repository.TransactionRepository;
import com.huggingsoft.pilot_main.repository.UserRepository;
import com.huggingsoft.pilot_main.service.ClientService;
import com.huggingsoft.pilot_main.service.exceptions.DataConflictException;
import com.huggingsoft.pilot_main.service.exceptions.ResourceNotFoundException;
import com.huggingsoft.pilot_main.service.exceptions.UnauthorizedOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

// --- Client Service Implementation (Similar pattern to Product) ---
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository; // For delete check
    private final ClientMapper clientMapper;

    // TODO: implement this method
    // Helper method to get main user (same as in ProductServiceImpl)
    private User getMainUserFromPrincipal(Object principal) {
        // ... implementation ...
        String username = "dummyUser"; // Placeholder
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        if (user.getParentUser() != null) {
            throw new UnauthorizedOperationException("Operation requires a main user account.");
        }
        return user;
    }

    // Helper to find client and verify ownership
    private Client findClientForUser(User user, UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + clientId));
        if (!client.getUser().getUserId().equals(user.getUserId())) {
            throw new UnauthorizedOperationException("Client does not belong to the authenticated user.");
        }
        return client;
    }

    @Override
    @Transactional
    public ClientResponseDTO createClient(Object principal, ClientCreateRequestDTO request) {
        User mainUser = getMainUserFromPrincipal(principal);
        // Add NIT uniqueness check if required per user (excluding C/F?)
        Client client = clientMapper.clientCreateRequestDTOToClient(request);
        client.setUser(mainUser);
        Client savedClient = clientRepository.save(client);
        return clientMapper.clientToClientResponseDTO(savedClient);
    }

    @Override
    public Page<ClientResponseDTO> findClients(Object principal, Boolean isActive, String search, Pageable pageable) {
        User mainUser = getMainUserFromPrincipal(principal);
        Specification<Client> spec = Specification.where(ClientSpecifications.belongsToUser(mainUser));
        if (isActive != null) {
            spec = spec.and(ClientSpecifications.isActive(isActive));
        }
        if (StringUtils.hasText(search)) {
            spec = spec.and(ClientSpecifications.nameOrNitOrEmailContains(search));
        }
        Page<Client> clientPage = clientRepository.findAll(spec, pageable);
        return clientPage.map(clientMapper::clientToClientResponseDTO);
    }

    @Override
    public ClientResponseDTO findClientById(Object principal, UUID clientId) {
        User mainUser = getMainUserFromPrincipal(principal);
        Client client = findClientForUser(mainUser, clientId);
        return clientMapper.clientToClientResponseDTO(client);
    }

    @Override
    @Transactional
    public ClientResponseDTO updateClient(Object principal, UUID clientId, ClientUpdateRequestDTO request) {
        User mainUser = getMainUserFromPrincipal(principal);
        Client client = findClientForUser(mainUser, clientId);
        // Update allowed fields
        if (request.getName() != null) client.setName(request.getName());
        if (request.getNit() != null) client.setNit(request.getNit()); // Add uniqueness check if needed
        if (request.getEmail() != null) client.setEmail(request.getEmail());
        if (request.getPhone() != null) client.setPhone(request.getPhone());
        if (request.getAddress() != null) client.setAddress(request.getAddress());
        if (request.getContext() != null) client.getContext().putAll(request.getContext());

        Client updatedClient = clientRepository.save(client);
        return clientMapper.clientToClientResponseDTO(updatedClient);
    }

    @Override
    @Transactional
    public void updateClientStatus(Object principal, UUID clientId, boolean isActive) {
        User mainUser = getMainUserFromPrincipal(principal);
        Client client = findClientForUser(mainUser, clientId);
        client.setActive(isActive);
        clientRepository.save(client);
    }

    @Override
    @Transactional
    public void deleteClient(Object principal, UUID clientId) {
        User mainUser = getMainUserFromPrincipal(principal);
        Client client = findClientForUser(mainUser, clientId);
        // Check if client used in transactions
        long usageCount = transactionRepository.count((root, query, cb) -> cb.equal(root.get("client"), client)); // Example spec count
        if(usageCount > 0) {
            throw new DataConflictException("Cannot delete client: It is referenced in " + usageCount + " transaction(s). Consider deactivating instead.");
        }
        clientRepository.delete(client);
    }

    // --- Mappers ---
//    private Client mapToClientEntity(ClientCreateRequestDTO dto){ /* ... */ return new Client();}
//    private ClientResponseDTO mapToClientResponse(Client entity){ /* ... */ return ClientResponseDTO.builder().clientId(entity.getClientId()).build();} // Implement fully

    // --- Specification Helper Class ---
    private static class ClientSpecifications {
        static Specification<Client> belongsToUser(User user) { /* ... */ return null;}
        static Specification<Client> isActive(boolean isActive) { /* ... */ return null;}
        static Specification<Client> nameOrNitOrEmailContains(String searchTerm) { /* ... */ return null;} // Implement search logic
    }
}
