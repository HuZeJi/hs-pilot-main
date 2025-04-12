package com.huggingsoft.pilot_main.service.impl;

import com.hsoft.model.dto.v1.users.ChangePasswordRequestDTO;
import com.hsoft.model.dto.v1.users.CompanyInfoUpdateRequestDTO;
import com.hsoft.model.dto.v1.users.SubUserCreateRequestDTO;
import com.hsoft.model.dto.v1.users.UserResponseDTO;
import com.hsoft.model.dto.v1.users.UserUpdateRequestDTO;
import com.hsoft.model.entities.v1.User;
import com.hsoft.model.mappers.UserMapper;
import com.huggingsoft.pilot_main.repository.UserRepository;
import com.huggingsoft.pilot_main.service.UserService;
import com.huggingsoft.pilot_main.service.exceptions.BusinessRuleViolationException;
import com.huggingsoft.pilot_main.service.exceptions.DataConflictException;
import com.huggingsoft.pilot_main.service.exceptions.ResourceNotFoundException;
import com.huggingsoft.pilot_main.service.exceptions.UnauthorizedOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.UUID;

// --- User Service Implementation ---
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Default to read-only transactions
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper; // Assuming a mapper for User entity to DTO conversion
    // Inject other repositories if needed for checks (e.g., TransactionRepository)

    // --- Helper Methods ---
    private User getUserFromPrincipal(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database"));
    }

    private User getMainUserFromPrincipal(String principalName) {
        User user = getUserFromPrincipal(principalName);
        if (user.getParentUser() != null) {
            throw new UnauthorizedOperationException("Operation requires a main user account.");
        }
        return user;
    }

    private User findSubUserForMain(User mainUser, UUID subUserId) {
        User subUser = userRepository.findById(subUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-user not found with ID: " + subUserId));
        // Verify ownership
        if (subUser.getParentUser() == null || !subUser.getParentUser().getUserId().equals(mainUser.getUserId())) {
            throw new UnauthorizedOperationException("Sub-user does not belong to the authenticated main user.");
        }
        return subUser;
    }


    // --- Public Methods ---
    @Override
    public UserResponseDTO getCurrentUser(String principalName) {
        User user = getUserFromPrincipal(principalName);
        return userMapper.userToUserResponseDTO(user);
    }

    @Transactional
    @Override
    public UserResponseDTO updateCurrentUser(String principalName, UserUpdateRequestDTO request) {
        User user = getUserFromPrincipal(principalName);

        // Update allowed fields (add more validation/logic as needed)
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            // Optional: Check email uniqueness if changing
            if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new DataConflictException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
            // Consider triggering email verification if email changes
        }
        if (request.getUsername() != null && !request.getUsername().equalsIgnoreCase(user.getUsername())) {
            if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
                throw new DataConflictException("Username already exists: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }
        if (request.getContext() != null) {
            // Merge context data carefully - simple overwrite shown here
            user.getContext().putAll(request.getContext());
        }

        User updatedUser = userRepository.save(user);
        return userMapper.userToUserResponseDTO(updatedUser);
    }

    @Transactional
    @Override
    public void changeCurrentUserPassword(String principalName, ChangePasswordRequestDTO request) {
        User user = getUserFromPrincipal(principalName);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleViolationException("Incorrect current password.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    @Override
    public UserResponseDTO updateCompanyInfo(String principal, CompanyInfoUpdateRequestDTO request) {
        User mainUser = getMainUserFromPrincipal(principal); // Ensures it's a main user

        // Update company fields
        if (request.getCompanyName() != null) mainUser.setCompanyName(request.getCompanyName());
        if (request.getCompanyAddress() != null) mainUser.setCompanyAddress(request.getCompanyAddress());
        if (request.getCompanyPhone() != null) mainUser.setCompanyPhone(request.getCompanyPhone());
        if (request.getCompanyNit() != null) {
            // Optional: Check NIT uniqueness if changing and value is different
            if (!request.getCompanyNit().equals(mainUser.getCompanyNit())) {
                // Add check logic if needed (e.g., query repository)
            }
            mainUser.setCompanyNit(request.getCompanyNit());
        }
        if (request.getContext() != null) {
            mainUser.getContext().putAll(request.getContext());
        }

        User updatedUser = userRepository.save(mainUser);
        return userMapper.userToUserResponseDTO(updatedUser);
    }

    @Transactional
    @Override
    public void deleteMainAccount(String principal) {
        User mainUser = getMainUserFromPrincipal(principal);
        // Add pre-delete checks if needed (e.g., ensure no active subscriptions?)
        // Be aware of cascade deletes defined in entities (products, clients etc. might be deleted too)
        userRepository.delete(mainUser);
    }

    // --- Sub-User Management ---

    @Transactional
    @Override
    public UserResponseDTO createSubUser(String principal, SubUserCreateRequestDTO request) {
        User mainUser = getMainUserFromPrincipal(principal);

        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new DataConflictException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DataConflictException("Email already exists: " + request.getEmail());
        }

        User subUser = new User();
        subUser.setUsername(request.getUsername());
        subUser.setEmail(request.getEmail());
        subUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        subUser.setParentUser(mainUser); // Link to main user
        subUser.setActive(true);
        subUser.setContext(request.getContext() != null ? request.getContext() : new HashMap<>());
        // Inherit company info? Or leave null/empty? Depends on requirements.
        subUser.setCompanyName(mainUser.getCompanyName()); // Example: Inherit company name

        User savedSubUser = userRepository.save(subUser);
        return userMapper.userToUserResponseDTO(savedSubUser);
    }

    @Override
    public Page<UserResponseDTO> findSubUsers(String principal, Boolean isActive, Pageable pageable) {
        User mainUser = getMainUserFromPrincipal(principal);

        // Use Specification for dynamic filtering
        Specification<User> spec = Specification.where(UserSpecifications.isSubUserOf(mainUser));
        if (isActive != null) {
            spec = spec.and(UserSpecifications.isActive(isActive));
        }

        Page<User> subUserPage = userRepository.findAll(spec, pageable);
        return subUserPage.map(userMapper::userToUserResponseDTO); // Map Page content
    }

    @Override
    public UserResponseDTO getSubUserById(String principal, UUID subUserId) {
        User mainUser = getMainUserFromPrincipal(principal);
        User subUser = findSubUserForMain(mainUser, subUserId); // Verifies ownership
        return userMapper.userToUserResponseDTO(subUser);
    }

    @Transactional
    @Override
    public UserResponseDTO updateSubUser(String principal, UUID subUserId, UserUpdateRequestDTO request) {
        User mainUser = getMainUserFromPrincipal(principal);
        User subUser = findSubUserForMain(mainUser, subUserId); // Verifies ownership

        // Update allowed fields (similar to updateCurrentUser, but on subUser)
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(subUser.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(request.getEmail())) { // Check global uniqueness
                throw new DataConflictException("Email already exists: " + request.getEmail());
            }
            subUser.setEmail(request.getEmail());
        }
        if (request.getUsername() != null && !request.getUsername().equalsIgnoreCase(subUser.getUsername())) {
            if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
                throw new DataConflictException("Username already exists: " + request.getUsername());
            }
            subUser.setUsername(request.getUsername());
        }
        if (request.getContext() != null) {
            subUser.getContext().putAll(request.getContext());
        }

        User updatedSubUser = userRepository.save(subUser);
        return userMapper.userToUserResponseDTO(updatedSubUser);
    }

    @Transactional
    @Override
    public void updateSubUserStatus(String principal, UUID subUserId, boolean isActive) {
        User mainUser = getMainUserFromPrincipal(principal);
        User subUser = findSubUserForMain(mainUser, subUserId); // Verifies ownership
        subUser.setActive(isActive);
        userRepository.save(subUser);
    }

    @Transactional
    @Override
    public void deleteSubUser(String principal, UUID subUserId) {
        User mainUser = getMainUserFromPrincipal(principal);
        User subUser = findSubUserForMain(mainUser, subUserId); // Verifies ownership
        // Add checks if sub-user created transactions? Depends on requirements (ON DELETE SET NULL used for created_by)
        userRepository.delete(subUser);
    }


    // --- Mappers ---
    // mapToUserResponse needed (can be same as in AuthServiceImpl or in a shared mapper)
//    private UserResponseDTO mapToUserResponse(User entity) {
//        if (entity == null) return null;
//        return UserResponseDTO.builder()
//                .userId(entity.getUserId())
//                .username(entity.getUsername())
//                .email(entity.getEmail())
//                .isActive(entity.isActive())
//                .createdAt(entity.getCreatedAt())
//                .updatedAt(entity.getUpdatedAt())
//                .context(entity.getContext())
//                .parentUserId(entity.getParentUser() != null ? entity.getParentUser().getUserId() : null)
//                .companyName(entity.getCompanyName())
//                .companyNit(entity.getCompanyNit())
//                .companyAddress(entity.getCompanyAddress())
//                .companyPhone(entity.getCompanyPhone())
//                .build();
//    }

    // --- Specification Helper Class (Inner or Separate File) ---
    private static class UserSpecifications {
        static Specification<User> isSubUserOf(User parent) {
            return (root, query, cb) -> cb.equal(root.get("parentUser"), parent);
        }
        static Specification<User> isActive(boolean isActive) {
            return (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
        }
        // Add more specifications as needed (e.g., for searching)
    }
}
