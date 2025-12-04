package com.umc.cardify.controller;

import com.umc.cardify.dto.auth.AuthResponse;
import com.umc.cardify.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "AuthController", description = "인증 및 토큰 관리 API")
public class AuthController {

    private final AuthService authService;
    
    @PostMapping("/refresh")
    @Operation(summary = "액세스 토큰 갱신", description = "리프레시 토큰으로 새로운 액세스 토큰을 발급합니다.")
    public ResponseEntity<AuthResponse.RefreshTokenRes> refresh(HttpServletRequest request) {
        AuthResponse.RefreshTokenRes response = authService.refreshAccessToken(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check-refresh-token")
    @Operation(summary = "리프레시 토큰 유효성 검증", description = "쿠키의 리프레시 토큰이 유효한지 검증합니다.")
    public ResponseEntity<AuthResponse.CheckRefreshTokenRes> checkRefreshToken(HttpServletRequest request) {
        AuthResponse.CheckRefreshTokenRes response = authService.checkRefreshToken(request);
        return ResponseEntity.ok(response);
    }
}