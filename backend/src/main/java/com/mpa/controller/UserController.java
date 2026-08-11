package com.mpa.controller;

import com.mpa.dto.UserRequest;
import com.mpa.dto.UserResponse;
import com.mpa.entity.Role;
import com.mpa.security.CustomUserDetails;
import com.mpa.service.UserService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Chỉ ADMIN được quản lý tài khoản người dùng. Chặn cả ở đây (@PreAuthorize class-level)
 * lẫn ở SecurityConfig (/api/users/** hasAuthority ROLE_ADMIN) — phòng thủ hai lớp.
 * Không có endpoint xoá cứng — dùng PATCH .../active để vô hiệu hoá thay vì xoá (xem
 * UserServiceImpl để biết lý do).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class UserController {

    private final UserService service;

    @GetMapping
    public ApiResponse<Page<UserResponse>> list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String maDonViCap6,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size) {
        try {
            return ApiResponse.ok(service.list(search, role, active, maDonViCap6, page, size));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải danh sách người dùng: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
                                             @RequestBody UserRequest request) {
        try {
            return ApiResponse.ok(service.create(request, principal.getUsername()));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tạo tài khoản: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@AuthenticationPrincipal CustomUserDetails principal,
                                             @PathVariable Long id,
                                             @RequestBody UserRequest request) {
        try {
            return ApiResponse.ok(service.update(id, request, principal.getUsername()));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi cập nhật tài khoản: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<UserResponse> setActive(@AuthenticationPrincipal CustomUserDetails principal,
                                                @PathVariable Long id,
                                                @RequestParam boolean active) {
        try {
            return ApiResponse.ok(service.setActive(id, active, principal.getUsername()));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }
}
