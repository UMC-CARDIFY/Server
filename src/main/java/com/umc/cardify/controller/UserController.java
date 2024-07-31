package com.umc.cardify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.config.KakaoProperties;
import com.umc.cardify.dto.user.UserRequest;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.repository.UserRepository;
import com.umc.cardify.service.UserService;

import com.umc.cardify.service.security.CustomOAuth2UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.cardify.domain.User;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
@Tag(name = "UserController", description = "회원가입, 로그인, 유저 프로필 관련 API")
@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final KakaoProperties kakaoProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final UserRepository userRepository;
    private final CustomOAuth2UserService customOAuth2UserService;

    @PostMapping(value = "/signup")
    @Operation(summary = "회원 가입", description = " 회원 가입 정보 입력, 성공 시 유저 이름 반환")
    public ResponseEntity<String> signup(@Validated @RequestBody UserRequest.signUp request) {
        String userName = userService.registerUser(request);

        return ResponseEntity.ok(userName);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "아이디와 비밀 번호 입력, 성공 시 토큰 반환")
    public ResponseEntity<UserResponse.tokenInfo> login(@Validated @RequestBody UserRequest.login request) {
        //UserResponse.tokenInfo tokenInfo = userService.login(request);
        UserResponse.tokenInfo tokenInfo = userService.login(request);

        return ResponseEntity.ok(tokenInfo);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "로그아웃 요청")
    public ResponseEntity<Void> logout() {
        userService.logout();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/oauth2/kakao")
    @Operation(summary = "카카오 로그인", description = "카카오 OAuth2 로그인")
    public ResponseEntity<UserResponse.tokenInfo> kakaoLogin(@RequestParam String code) throws IOException {
        // 카카오 서버에서 액세스 토큰을 얻기 위한 요청
        String tokenUrl = "https://kauth.kakao.com/oauth/token";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        String requestBody = String.format(
                "grant_type=authorization_code&client_id=%s&redirect_uri=%s&code=%s",
                kakaoProperties.getClientId(),
                kakaoProperties.getRedirectUri(),
                code
        );

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(tokenUrl, HttpMethod.POST, requestEntity, String.class);

        // 액세스 토큰을 포함한 응답에서 JSON 파싱
        JsonNode responseNode = objectMapper.readTree(responseEntity.getBody());
        String accessToken = responseNode.get("access_token").asText();

        // 액세스 토큰을 사용하여 사용자 정보를 가져오는 요청
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";
        HttpHeaders userInfoHeaders = new HttpHeaders();
        userInfoHeaders.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> userInfoRequestEntity = new HttpEntity<>(userInfoHeaders);
        ResponseEntity<String> userInfoResponseEntity = restTemplate.exchange(userInfoUrl, HttpMethod.GET, userInfoRequestEntity, String.class);

        JsonNode userInfoNode = objectMapper.readTree(userInfoResponseEntity.getBody());
        String email = userInfoNode.path("kakao_account").path("email").asText();
        String name = userInfoNode.path("properties").path("nickname").asText();

        // 사용자 정보 등록 또는 조회
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userService.registerUser(email, name));

        // JWT 토큰 생성
        UserResponse.tokenInfo tokenInfo = userService.createToken(user);

        return ResponseEntity.ok(tokenInfo);
    }

    @GetMapping("/me")
    public ResponseEntity<Authentication> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(authentication);
    }

}
