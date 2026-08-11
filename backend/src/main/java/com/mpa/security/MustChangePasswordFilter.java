package com.mpa.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mpa.util.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Chạy SAU JwtAuthenticationFilter. Tài khoản còn cờ mustChangePassword=true thì mọi request
 * (trừ allowlist dưới) bị chặn 403 kèm errorCode=MUST_CHANGE_PASSWORD, buộc phải gọi
 * /api/auth/change-password trước khi dùng được API khác.
 */
@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final List<String> ALLOWLIST = List.of(
            "/api/auth/change-password", "/api/auth/me", "/api/auth/logout",
            "/api/auth/login", "/api/auth/refresh"
    );

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();

        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails
                && userDetails.getUser().isMustChangePassword()
                && ALLOWLIST.stream().noneMatch(path::startsWith)) {

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            ApiResponse<Void> body = ApiResponse.error("Bạn phải đổi mật khẩu trước khi tiếp tục", "MUST_CHANGE_PASSWORD");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
