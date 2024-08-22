package com.umc.cardify.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.service.KakaoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.Map;


@Tag(name = "OAuth2Controller", description = "카카오 로그인 관련 API")
@RestController
@RequestMapping("/api/v1/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final KakaoService kakaoService;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;

    // 인증 관련
    @GetMapping("/authorization/kakao")
    public ResponseEntity<Map<String, String>> getKakaoAuthorizationUrl() {
        String kakaoAuthUrl = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", clientId)
                //.queryParam("client-secret", clientSecret)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .toUriString();

        return ResponseEntity.ok(Collections.singletonMap("authorizationUrl", kakaoAuthUrl));
    }

    // 콜백
    @PostMapping("callback/kakao")
    public ResponseEntity<UserResponse.tokenInfo> kakaoCallback(@RequestBody Map<String, String> payload, HttpServletRequest request) throws JsonProcessingException {
        System.out.println("Received kakao callback request. Request ID: " + request.getSession().getId());
        try {
            String code = payload.get("code");
            if (code == null || code.isEmpty()) {
                throw new IllegalArgumentException("Code is missing or empty");
            }
            System.out.println("Processing login for code: " + code);
            UserResponse.tokenInfo tokenInfo = kakaoService.processKakaoLogin(code);
            return ResponseEntity.ok(tokenInfo);
        } catch (HttpClientErrorException e) {
            System.out.println("Kakao API error: {}" + e.getResponseBodyAsString());
            throw new RuntimeException("카카오 로그인 처리 중 오류가 발생했습니다: " + e.getStatusCode());
        } catch (Exception e) {
            System.out.println("Error in kakao callback: " + e);
            return ResponseEntity.badRequest().body(new UserResponse.tokenInfo("Bearer", "Error: " + e.getMessage(), "null"));
        }
    }
}
