package com.umc.cardify.controller;

import com.umc.cardify.domain.User;
import com.umc.cardify.dto.user.UserRequest;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Tag(name = "UserController", description = "회원가입, 로그인, 유저 프로필 관련 API")
@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping(value = "/signIn")
    @Operation(summary = "회원 가입", description = " 회원 가입 정보 입력, 성공 시 유저 이름 반환")
    public ResponseEntity<String> signIn(@Validated @RequestBody UserRequest.signIn request) {
        String userName = userService.registerUser(request);

        return ResponseEntity.ok(userName);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "아이디와 비밀 번호 입력, 성공 시 토큰 반환")
    public ResponseEntity<UserResponse.tokenInfo> login(@Validated @RequestBody UserRequest.login request) {
        UserResponse.tokenInfo tokenInfo = userService.login(request);

        return ResponseEntity.ok(tokenInfo);
    }

}
