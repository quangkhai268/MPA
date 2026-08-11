package com.mpa.dto;

import com.mpa.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private Role role;
    private String maDonViCap6;
    private boolean active;
    private boolean mustChangePassword;
    private LocalDateTime createdAt;
}
