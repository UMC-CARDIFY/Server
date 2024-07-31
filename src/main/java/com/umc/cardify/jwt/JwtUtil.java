package com.umc.cardify.jwt;

import java.util.Base64;
import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.dto.user.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUtil {
    @Value("${jwt.accessTokenValidity}")
    long accessTokenValidity;

    @Value("${jwt.refreshTokenValidity}")
    long refreshTokenValidity;

    @Value("${jwt.secret}")
    private String secretKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserResponse.tokenInfo generateTokens(Long userId) {
        String accessToken = createAccessToken(userId);
        String refreshToken = createRefreshToken(userId);

        return UserResponse.tokenInfo.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public String createAccessToken(Long userId) {
        Claims claims = Jwts.claims();
        claims.put("userId", userId);
        claims.put("type", "Access");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject("AccessToken")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenValidity))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Claims claims = Jwts.claims();
        claims.put("userId", userId);
        claims.put("type", "Refresh");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject("RefreshToken")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenValidity))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }

    public String refreshAccessToken(String refreshToken) {
        Claims claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(refreshToken).getBody();
        Long userId = claims.get("userId", Long.class);

        return createAccessToken(userId);
    }

    public Long extractUserId(String token) {
        Base64.Decoder decoder = Base64.getDecoder();
        String[] splitJwt = token.split("\\.");

        if (splitJwt.length < 2) {
            log.error("Invalid JWT token structure.");
            throw new IllegalArgumentException("Invalid JWT token structure.");
        }

        String payload = new String(decoder.decode(splitJwt[1]
            .replace("-", "+")
            .replace("_", "/")));

        log.debug("Decoded JWT payload: {}", payload);

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            Long userId = jsonNode.get("userId").asLong();
            log.info("Extracted userId: {}", userId);
            return userId;
        } catch (Exception e) {
            log.error("Failed to extract userId from JWT payload.", e);
            throw new RuntimeException("Failed to extract userId from JWT payload.", e);
        }
    }

}
