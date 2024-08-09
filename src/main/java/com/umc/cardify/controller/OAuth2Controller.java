//package com.umc.cardify.controller;
//
//import com.umc.cardify.dto.user.UserResponse;
//import com.umc.cardify.jwt.JwtUtil;
//import com.umc.cardify.service.security.CustomOAuth2UserService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.web.bind.annotation.*;
//
//@Slf4j
//@Tag(name = "OAuth2Controller", description = "카카오 로그인 관련 API")
//@RestController
//@RequestMapping("/api/v1/auth/oauth2")
//@RequiredArgsConstructor
//public class OAuth2Controller {
//
//    private final CustomOAuth2UserService customOAuth2UserService;
//    private final JwtUtil jwtUtil;
//
//    // 카카오 로그인
//    @GetMapping("/kakao")
//    @Operation(summary = "카카오 로그인 API") public ResponseEntity<UserResponse.tokenInfo> login(@RequestBody OAuth2UserRequest userRequest) {
//        log.info(userRequest.toString());
//        OAuth2User kakaoUser = customOAuth2UserService.loadUser(userRequest);
//        UserResponse.tokenInfo tokenInfo = jwtUtil.generateTokens(kakaoUser.getAttribute("id"));
//        return ResponseEntity.ok(tokenInfo);
//    }
//}
//
