package com.mpa.controller;

import com.mpa.dto.AuthResponse;
import com.mpa.dto.ChangePasswordRequest;
import com.mpa.dto.LoginRequest;
import com.mpa.dto.RefreshTokenRequest;
import com.mpa.dto.UserResponse;
import com.mpa.security.CustomUserDetails;
import com.mpa.service.AuthService;
import com.mpa.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ApiResponse.ok(authService.login(request));
        } catch (Exception e) {
            // Không lộ chi tiết (sai username hay sai password hay tài khoản bị khoá) —
            // tránh dò tài khoản tồn tại.
            return ApiResponse.error("Tên đăng nhập hoặc mật khẩu không đúng");
        }
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            return ApiResponse.ok(authService.refresh(request));
        } catch (Exception e) {
            return ApiResponse.error("Không thể làm mới phiên đăng nhập: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
        try {
            return ApiResponse.ok(authService.me(principal.getUsername()));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải thông tin tài khoản: " + e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal CustomUserDetails principal,
                                             @Valid @RequestBody ChangePasswordRequest request) {
        try {
            authService.changePassword(principal.getUsername(), request);
            return ApiResponse.ok(null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // Phase 1: chưa có danh sách đen để revoke token thật — endpoint này chỉ để
        // frontend gọi trước khi tự xoá token cục bộ. Hook lại cho Phase 2 (revoke thật).
        return ApiResponse.ok(null);
    }
}
