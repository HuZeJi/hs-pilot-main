package com.huggingsoft.pilot_main.service;

import com.hsoft.model.dto.v1.users.LoginRequestDTO;
import com.hsoft.model.dto.v1.users.LoginResponseDTO;
import com.hsoft.model.dto.v1.users.RegisterRequestDTO;
import com.hsoft.model.dto.v1.users.UserResponseDTO;

// --- Auth Service ---
public interface AuthService {
    UserResponseDTO registerMainUser(RegisterRequestDTO request);
    LoginResponseDTO login(LoginRequestDTO request);
    void requestPasswordReset(String email);
    void confirmPasswordReset(String token, String newPassword);
    // void logout(Object principal); // If server-side invalidation needed
}
