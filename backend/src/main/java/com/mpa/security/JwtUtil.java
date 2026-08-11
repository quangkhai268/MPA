package com.mpa.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Sinh/verify JWT access + refresh token. Claim "typ" (access/refresh) chặn refresh token
 * bị dùng thay access token — parse ra "refresh" thì JwtAuthenticationFilter bỏ qua, không
 * cho xác thực bằng nó.
 */
@Component
public class JwtUtil {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_UID = "uid";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long accessExpirationMs;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret chưa được cấu hình hoặc quá ngắn (cần >= 32 byte). " +
                    "Set biến môi trường JWT_SECRET trước khi chạy production (xem CLAUDE.md).");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(CustomUserDetails userDetails) {
        return buildToken(userDetails, TYPE_ACCESS, accessExpirationMs);
    }

    public String generateRefreshToken(CustomUserDetails userDetails) {
        return buildToken(userDetails, TYPE_REFRESH, refreshExpirationMs);
    }

    private String buildToken(CustomUserDetails userDetails, String type, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim(CLAIM_ROLE, userDetails.getUser().getRole().name())
                .claim(CLAIM_UID, userDetails.getUser().getId())
                .claim(CLAIM_TYPE, type)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, c -> c.get(CLAIM_TYPE, String.class));
    }

    public boolean isTokenValid(String token, CustomUserDetails userDetails) {
        try {
            return extractUsername(token).equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return resolver.apply(claims);
    }
}
