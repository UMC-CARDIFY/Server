package com.umc.cardify.controller;

import com.umc.cardify.dto.user.UserRequest;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.service.UserService;
import io.jsonwebtoken.Jwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@Tag(name = "UserController", description = "회원가입, 로그인, 유저 프로필 관련 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 이메일 회원가입
    @PostMapping("/signup")
    @Operation(summary = "회원가입 API")
    @ApiResponse(responseCode = "200", description = "회원가입 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json"))
    public ResponseEntity<String> signUp(@RequestBody UserRequest.signUp signUpRequest) {
        String email = userService.signup(signUpRequest);
        return ResponseEntity.ok(email);
    }

    // 이메일 로그인
    @PostMapping("/login")
    @Operation(summary = "로그인 API")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json"))
    public ResponseEntity<UserResponse.tokenInfo> login(@RequestBody UserRequest.login loginRequest) {
        UserResponse.tokenInfo tokenInfo = userService.login(loginRequest);
        return ResponseEntity.ok(tokenInfo);
    }

    // 카카오 로그인

}

