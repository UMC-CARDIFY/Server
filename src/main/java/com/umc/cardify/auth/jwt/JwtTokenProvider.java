package com.umc.cardify.auth.jwt;

import com.umc.cardify.domain.enums.AuthProvider;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.accessTokenValidity}")
    private long accessTokenValidity;

    @Value("${jwt.refreshTokenValidity}")
    private long refreshTokenValidity;

    // 일관된 키 생성 메서드
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // Access Token 생성
    public String createAccessToken(String email, AuthProvider provider) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidity);

        return Jwts.builder()
            .setSubject(email)
            .claim("provider", provider.toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken() {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidity);

        return Jwts.builder()
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
    }

    // Token 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token", e);
        } catch (JwtException e) {
            log.error("Invalid JWT token", e);
        }
        return false;
    }

    // Token에서 이메일 추출
    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            return claims.getSubject();
        } catch (JwtException e) {
            log.error("Error extracting email from token", e);
            throw new JwtTokenInvalidException("Invalid JWT token", e);
        }
    }

    // 토큰에서 제공자 정보 추출
    public AuthProvider getProviderFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

            return AuthProvider.valueOf(claims.get("provider", String.class));
        } catch (JwtException e) {
            log.error("Error extracting provider from token", e);
            throw new JwtTokenInvalidException("Invalid JWT token", e);
        }
    }

    // 커스텀 예외 클래스 추가
    public static class JwtTokenInvalidException extends RuntimeException {
        public JwtTokenInvalidException(String message) {
            super(message);
        }

        public JwtTokenInvalidException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}