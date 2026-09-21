package com.mpa.service.impl;

import com.mpa.dto.AuthResponse;
import com.mpa.dto.ChangePasswordRequest;
import com.mpa.dto.LoginRequest;
import com.mpa.dto.RefreshTokenRequest;
import com.mpa.dto.UserResponse;
import com.mpa.entity.User;
import com.mpa.repository.PhongBanRepository;
import com.mpa.repository.UserRepository;
import com.mpa.security.CustomUserDetails;
import com.mpa.security.JwtUtil;
import com.mpa.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PhongBanRepository phongBanRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse login(LoginRequest request) {
        var authToken = new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        var authentication = authenticationManager.authenticate(authToken);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return buildAuthResponse(userDetails);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!JwtUtil.TYPE_REFRESH.equals(jwtUtil.extractTokenType(token)) || jwtUtil.isTokenExpired(token)) {
            throw new BadCredentialsException("Refresh token không hợp lệ hoặc đã hết hạn");
        }
        String username = jwtUtil.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Tài khoản không tồn tại"));
        if (!user.isActive()) {
            throw new BadCredentialsException("Tài khoản đã bị vô hiệu hoá");
        }
        return buildAuthResponse(new CustomUserDetails(user));
    }

    @Override
    public UserResponse me(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        return toResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(CustomUserDetails userDetails) {
        return AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(userDetails))
                .refreshToken(jwtUtil.generateRefreshToken(userDetails))
                .tokenType("Bearer")
                .user(toResponse(userDetails.getUser()))
                .build();
    }

    private UserResponse toResponse(User u) {
        String tenDonViCap6 = u.getMaDonViCap6() == null ? null
                : phongBanRepository.findFirstByMaDonViCap6(u.getMaDonViCap6())
                        .map(com.mpa.entity.PhongBan::getTenDonViCap6).orElse(null);
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .maDonViCap6(u.getMaDonViCap6())
                .tenDonViCap6(tenDonViCap6)
                .active(u.isActive())
                .mustChangePassword(u.isMustChangePassword())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
