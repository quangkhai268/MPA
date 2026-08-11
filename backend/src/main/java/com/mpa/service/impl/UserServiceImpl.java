package com.mpa.service.impl;

import com.mpa.dto.UserRequest;
import com.mpa.dto.UserResponse;
import com.mpa.entity.Role;
import com.mpa.entity.User;
import com.mpa.repository.UserRepository;
import com.mpa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<UserResponse> list(String search, Role role, Boolean active, String maDonViCap6, int page, int size) {
        String s = search == null ? "" : search.trim();
        return repo.search(s, role, active, maDonViCap6, PageRequest.of(page, size)).map(this::toResponse);
    }

    @Override
    public UserResponse create(UserRequest req, String createdBy) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập tên đăng nhập");
        }
        if (repo.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        if (req.getPassword() == null || req.getPassword().length() < 8) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự");
        }
        User entity = new User();
        entity.setUsername(req.getUsername());
        applyRequest(entity, req);
        entity.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        // Luôn ép true phía server bất kể client gửi gì — tài khoản mới luôn phải đổi mật khẩu.
        entity.setMustChangePassword(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedBy(createdBy);
        entity.setUpdatedBy(createdBy);
        repo.save(entity);
        return toResponse(entity);
    }

    @Override
    public UserResponse update(Long id, UserRequest req, String updatedBy) {
        User entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản id=" + id));

        boolean losingAdminStatus = entity.getRole() == Role.ROLE_ADMIN && entity.isActive()
                && (req.getRole() != Role.ROLE_ADMIN || !req.isActive());
        if (losingAdminStatus) {
            guardLastAdmin();
        }

        applyRequest(entity, req);
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            if (req.getPassword().length() < 8) {
                throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự");
            }
            // Admin reset mật khẩu cho user khác — cũng ép đổi lại ngay lần đăng nhập sau.
            entity.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            entity.setMustChangePassword(true);
        }
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(updatedBy);
        repo.save(entity);
        return toResponse(entity);
    }

    @Override
    public UserResponse setActive(Long id, boolean active, String updatedBy) {
        User entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản id=" + id));
        if (!active && entity.getRole() == Role.ROLE_ADMIN && entity.isActive()) {
            guardLastAdmin();
        }
        entity.setActive(active);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(updatedBy);
        repo.save(entity);
        return toResponse(entity);
    }

    /** Chặn vô hiệu hoá/hạ quyền quản trị viên ADMIN đang active cuối cùng — tránh khoá cứng
     *  cả hệ thống (không còn ai đủ quyền tạo lại tài khoản admin). */
    private void guardLastAdmin() {
        if (repo.countByRoleAndActiveTrue(Role.ROLE_ADMIN) <= 1) {
            throw new IllegalStateException("Không thể vô hiệu hoá/hạ quyền quản trị viên cuối cùng");
        }
    }

    private void applyRequest(User entity, UserRequest req) {
        entity.setFullName(req.getFullName());
        entity.setEmail(req.getEmail());
        entity.setRole(req.getRole());
        entity.setMaDonViCap6(req.getMaDonViCap6());
        entity.setActive(req.isActive());
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .maDonViCap6(u.getMaDonViCap6())
                .active(u.isActive())
                .mustChangePassword(u.isMustChangePassword())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
