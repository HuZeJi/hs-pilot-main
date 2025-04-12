package com.huggingsoft.pilot_main.service;

import com.hsoft.model.dto.v1.clients.ClientCreateRequestDTO;
import com.hsoft.model.dto.v1.clients.ClientResponseDTO;
import com.hsoft.model.dto.v1.clients.ClientUpdateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

// --- Client Service ---
public interface ClientService {
    ClientResponseDTO createClient(Object principal, ClientCreateRequestDTO request);
    Page<ClientResponseDTO> findClients(Object principal, Boolean isActive, String search, Pageable pageable);
    ClientResponseDTO findClientById(Object principal, UUID clientId);
    ClientResponseDTO updateClient(Object principal, UUID clientId, ClientUpdateRequestDTO request);
    void updateClientStatus(Object principal, UUID clientId, boolean isActive);
    void deleteClient(Object principal, UUID clientId);
}
