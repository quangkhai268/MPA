package com.mpa.service;

import com.mpa.dto.UserRequest;
import com.mpa.dto.UserResponse;
import com.mpa.entity.Role;
import org.springframework.data.domain.Page;

public interface UserService {
    Page<UserResponse> list(String search, Role role, Boolean active, String maDonViCap6, int page, int size);
    UserResponse create(UserRequest request, String createdBy);
    UserResponse update(Long id, UserRequest request, String updatedBy);
    UserResponse setActive(Long id, boolean active, String updatedBy);
}
