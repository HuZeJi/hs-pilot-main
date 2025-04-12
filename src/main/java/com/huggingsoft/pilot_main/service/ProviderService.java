package com.huggingsoft.pilot_main.service;

import com.hsoft.model.dto.v1.providers.ProviderCreateRequestDTO;
import com.hsoft.model.dto.v1.providers.ProviderResponseDTO;
import com.hsoft.model.dto.v1.providers.ProviderUpdateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

// --- Provider Service ---
public interface ProviderService {
    ProviderResponseDTO createProvider(Object principal, ProviderCreateRequestDTO request);
    Page<ProviderResponseDTO> findProviders(Object principal, Boolean isActive, String search, Pageable pageable);
    ProviderResponseDTO findProviderById(Object principal, UUID providerId);
    ProviderResponseDTO updateProvider(Object principal, UUID providerId, ProviderUpdateRequestDTO request);
    void updateProviderStatus(Object principal, UUID providerId, boolean isActive);
    void deleteProvider(Object principal, UUID providerId);
}
