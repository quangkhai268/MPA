package com.mpa.dto;

import com.mpa.entity.Role;
import lombok.Data;

@Data
public class UserRequest {
    private String username;
    private String fullName;
    private String email;
    private Role role;
    private String maDonViCap6;
    /** Rỗng/null khi update = không đổi mật khẩu. Bắt buộc khi create (kiểm tra ở service). */
    private String password;
    private boolean active = true;
}
