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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag(name = "OAuth2Controller", description = "카카오 로그인 관련 API")
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final KakaoService kakaoService;
    private static final Logger log = LoggerFactory.getLogger(OAuth2Controller.class);

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;

    // 인증 관련
    @GetMapping("/api/v1/oauth2/authorization/kakao")
    public ResponseEntity<Map<String, String>> getKakaoAuthorizationUrl() {
        String kakaoAuthUrl = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .toUriString();
        return ResponseEntity.ok(Collections.singletonMap("authorizationUrl", kakaoAuthUrl));
    }

    // 콜백
    @GetMapping("oauth2/callback/kakao")
    public ResponseEntity<UserResponse.tokenInfo> kakaoCallback(@RequestParam String code, HttpServletRequest request) throws JsonProcessingException {
        log.info("Received kakao callback request. Request ID: {}", request.getSession().getId());
        try {
            log.info("Processing login for code: {}", code);
            UserResponse.tokenInfo tokenInfo = kakaoService.processKakaoLogin(code);
            return ResponseEntity.ok(tokenInfo);
        } catch (HttpClientErrorException e) {
            log.error("Kakao API error: {}", e.getResponseBodyAsString());
            throw new RuntimeException("카카오 로그인 처리 중 오류가 발생했습니다: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Error in kakao callback: ", e);
            return ResponseEntity.badRequest().body(new UserResponse.tokenInfo("Bearer", "Error: " + e.getMessage(), "null"));
        }
    }
}
