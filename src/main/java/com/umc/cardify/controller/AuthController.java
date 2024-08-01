package com.umc.cardify.controller;

import com.umc.cardify.dto.user.UserRequest;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.service.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthController(AuthenticationManager authenticationManager, CustomUserDetailsService customUserDetailsService) {
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
    }

//    @PostMapping("/login")
//    @Operation(summary = "로그인", description = "아이디와 비밀번호를 입력하여 로그인합니다.")
//    public ResponseEntity<UserResponse.TokenInfo> login(@Validated @RequestBody UserRequest.login request) {
//        Authentication authentication = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//
//        UserResponse.TokenInfo tokenInfo = customUserDetailsService.generateToken(authentication);
//
//        return ResponseEntity.ok(tokenInfo);
//    }
}
