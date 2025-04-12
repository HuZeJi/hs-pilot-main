package com.huggingsoft.pilot_main.service;

import com.hsoft.model.dto.v1.users.ChangePasswordRequestDTO;
import com.hsoft.model.dto.v1.users.CompanyInfoUpdateRequestDTO;
import com.hsoft.model.dto.v1.users.SubUserCreateRequestDTO;
import com.hsoft.model.dto.v1.users.UserResponseDTO;
import com.hsoft.model.dto.v1.users.UserUpdateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

// --- User Service ---
public interface UserService {

    // --- Public Methods ---
    UserResponseDTO getCurrentUser(String principalName);

    UserResponseDTO updateCurrentUser(String principal, UserUpdateRequestDTO request);
    void changeCurrentUserPassword(String principal, ChangePasswordRequestDTO request);

    UserResponseDTO updateCompanyInfo(String principal, CompanyInfoUpdateRequestDTO request);
    void deleteMainAccount(String principal /*, confirmation? */);

    // Sub-User Management
    UserResponseDTO createSubUser(String principal, SubUserCreateRequestDTO request);
    Page<UserResponseDTO> findSubUsers(String principal, Boolean isActive, Pageable pageable);
    UserResponseDTO getSubUserById(String principal, UUID subUserId);
    UserResponseDTO updateSubUser(String principal, UUID subUserId, UserUpdateRequestDTO request);
    void updateSubUserStatus(String principal, UUID subUserId, boolean isActive);
    void deleteSubUser(String principal, UUID subUserId);

}
