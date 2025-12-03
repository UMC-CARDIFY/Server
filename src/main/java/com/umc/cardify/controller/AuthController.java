package com.umc.cardify.controller;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.domain.User;
import com.umc.cardify.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.Cookie;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "AuthController", description = "토큰 관리 API")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;


    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        // 쿠키에서 리프레시 토큰 추출
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        // 리프레시 토큰이 없는 경우
        if (refreshToken == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "리프레시 토큰이 없습니다.");
            return ResponseEntity.badRequest().body(error);
        }

        // 리프레시 토큰 검증
        if (!tokenProvider.validateToken(refreshToken)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "유효하지 않은 리프레시 토큰입니다.");
            return ResponseEntity.badRequest().body(error);
        }

        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("해당 리프레시 토큰의 사용자를 찾을 수 없습니다."));

        // 새로운 액세스 토큰 발급
        String newAccessToken = tokenProvider.createAccessToken(
                user.getEmail(),
                user.getProvider()
        );

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);

        return ResponseEntity.ok(tokens);
    }

    // 리프래시 토큰 유효성 검증
    @GetMapping("/check-refresh-token")
    public ResponseEntity<Map<String, Boolean>> checkRefreshToken(HttpServletRequest request) {
        Map<String, Boolean> response = new HashMap<>();

        // 쿠키에서 리프레시 토큰 찾기
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        // 토큰 유효성 검사
        boolean isValid = false;
        if (refreshToken != null) {
            try {
                isValid = tokenProvider.validateToken(refreshToken);
            } catch (Exception e) {
                isValid = false;
            }
        }

        response.put("valid", isValid);
        return ResponseEntity.ok(response);
    }
}