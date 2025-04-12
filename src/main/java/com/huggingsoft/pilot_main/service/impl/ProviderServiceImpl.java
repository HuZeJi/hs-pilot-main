package com.huggingsoft.pilot_main.service.impl;

import com.hsoft.model.dto.v1.providers.ProviderCreateRequestDTO;
import com.hsoft.model.dto.v1.providers.ProviderResponseDTO;
import com.hsoft.model.dto.v1.providers.ProviderUpdateRequestDTO;
import com.hsoft.model.entities.v1.Provider;
import com.hsoft.model.entities.v1.User;
import com.hsoft.model.mappers.ProviderMapper;
import com.huggingsoft.pilot_main.repository.ProviderRepository;
import com.huggingsoft.pilot_main.repository.TransactionRepository;
import com.huggingsoft.pilot_main.repository.UserRepository;
import com.huggingsoft.pilot_main.service.ProviderService;
import com.huggingsoft.pilot_main.service.exceptions.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

// --- Provider Service Implementation (Similar pattern to Client) ---
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProviderServiceImpl implements ProviderService {

    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final ProviderMapper providerMapper;

    // Helper method to get main user
    private User getMainUserFromPrincipal(Object principal) { /* ... */ return null;}
    // Helper to find provider and verify ownership
    private Provider findProviderForUser(User user, UUID providerId) { /* ... */ return null;}

    @Override
    @Transactional
    public ProviderResponseDTO createProvider(Object principal, ProviderCreateRequestDTO request) {
        User mainUser = getMainUserFromPrincipal(principal);
        // Check NIT uniqueness for this user
        if (StringUtils.hasText(request.getNit()) && providerRepository.existsByUserAndNit(mainUser, request.getNit())) {
            throw new DataConflictException("Provider NIT already exists for this user: " + request.getNit());
        }
        Provider provider = providerMapper.providerCreateRequestDTOToProvider(request);
        provider.setUser(mainUser);
        Provider savedProvider = providerRepository.save(provider);
        return providerMapper.providerToProviderResponseDTO(savedProvider);
    }

    @Override
    public Page<ProviderResponseDTO> findProviders(Object principal, Boolean isActive, String search, Pageable pageable) {
        User mainUser = getMainUserFromPrincipal(principal);
        Specification<Provider> spec = Specification.where(ProviderSpecifications.belongsToUser(mainUser));
        // Add isActive and search specifications
        Page<Provider> providerPage = providerRepository.findAll(spec, pageable);
        return providerPage.map(providerMapper::providerToProviderResponseDTO);
    }

    @Override
    public ProviderResponseDTO findProviderById(Object principal, UUID providerId) {
        User mainUser = getMainUserFromPrincipal(principal);
        Provider provider = findProviderForUser(mainUser, providerId);
        return providerMapper.providerToProviderResponseDTO(provider);
    }

    @Override
    @Transactional
    public ProviderResponseDTO updateProvider(Object principal, UUID providerId, ProviderUpdateRequestDTO request) {
        User mainUser = getMainUserFromPrincipal(principal);
        Provider provider = findProviderForUser(mainUser, providerId);
        // Check NIT uniqueness if changed
        if (StringUtils.hasText(request.getNit()) && !request.getNit().equals(provider.getNit())) {
            if (providerRepository.existsByUserAndNit(mainUser, request.getNit())) {
                throw new DataConflictException("Provider NIT already exists for this user: " + request.getNit());
            }
            provider.setNit(request.getNit());
        }
        // Update other fields
        // ...
        if (request.getContext() != null) provider.getContext().putAll(request.getContext());
        Provider updatedProvider = providerRepository.save(provider);
        return providerMapper.providerToProviderResponseDTO(updatedProvider);
    }

    @Override
    @Transactional
    public void updateProviderStatus(Object principal, UUID providerId, boolean isActive) {
        User mainUser = getMainUserFromPrincipal(principal);
        Provider provider = findProviderForUser(mainUser, providerId);
        provider.setActive(isActive);
        providerRepository.save(provider);
    }

    @Override
    @Transactional
    public void deleteProvider(Object principal, UUID providerId) {
        User mainUser = getMainUserFromPrincipal(principal);
        Provider provider = findProviderForUser(mainUser, providerId);
        // Check if provider used in transactions
        long usageCount = transactionRepository.count((root, query, cb) -> cb.equal(root.get("provider"), provider));
        if(usageCount > 0) {
            throw new DataConflictException("Cannot delete provider: It is referenced in " + usageCount + " transaction(s). Consider deactivating instead.");
        }
        providerRepository.delete(provider);
    }

    // --- Mappers ---
//    private Provider mapToProviderEntity(ProviderCreateRequestDTO dto){ /* ... */ return new Provider();}
//    private ProviderResponseDTO mapToProviderResponse(Provider entity){ /* ... */ return ProviderResponseDTO.builder().providerId(entity.getProviderId()).build();} // Implement fully

    // --- Specification Helper Class ---
    private static class ProviderSpecifications {
        static Specification<Provider> belongsToUser(User user) { /* ... */ return null;}
        // Add isActive, search specifications
    }
}
