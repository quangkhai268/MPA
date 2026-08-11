package com.mpa.service;

import com.mpa.dto.AuthResponse;
import com.mpa.dto.ChangePasswordRequest;
import com.mpa.dto.LoginRequest;
import com.mpa.dto.RefreshTokenRequest;
import com.mpa.dto.UserResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshTokenRequest request);
    UserResponse me(String username);
    void changePassword(String username, ChangePasswordRequest request);
}
