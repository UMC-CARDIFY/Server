package com.umc.cardify.controller;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.dto.user.UserRequest;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.service.UserService;
import io.jsonwebtoken.Jwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@Tag(name = "UserController", description = "회원가입, 로그인, 유저 프로필 관련 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    @Operation(summary = "회원가입 API")
    @ApiResponse(responseCode = "200", description = "회원가입 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json"))
    public ResponseEntity<?> signUp(@Validated @RequestBody UserRequest.signUp signUpRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // 유효성 검사 실패 시 오류 메시지 반환
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            String name = userService.signup(signUpRequest);
            return ResponseEntity.ok(name);
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원가입 중 오류 발생");
        }
    }

    // 이메일 로그인
    @PostMapping("/login")
    @Operation(summary = "로그인 API")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json"))
    public ResponseEntity<UserResponse.tokenInfo> login(@Validated @RequestBody UserRequest.login loginRequest) {
        UserResponse.tokenInfo tokenInfo = userService.login(loginRequest);
        return ResponseEntity.ok(tokenInfo);
    }

    // 로그아웃
    @PostMapping("/logout")
    @Operation(summary = "로그아웃 API")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json"))
    public ResponseEntity<UserResponse.LogoutResponse> logout(@RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.extractUserId(token);
            userService.logout(userId);
            return ResponseEntity.ok(new UserResponse.LogoutResponse("로그아웃 되었습니다.", true));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(new UserResponse.LogoutResponse(e.getMessage(), false));
        }
    }

    // 마이페이지 조회
    @GetMapping("/mypage")
    @Operation(summary = "마이페이지 정보 조회 API")
    @ApiResponse(responseCode = "200", description = "마이페이지 정보 조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json"))
    public ResponseEntity<UserResponse.MyPageInfo> getMyPageInfo(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token);
        UserResponse.MyPageInfo myPageInfo = userService.getMyPageInfo(userId);
        return ResponseEntity.ok(myPageInfo);
    }

    // 프로필 이미지 수정
    @PutMapping("/profile-image")
    @Operation(summary = "프로필 이미지 수정 API")
    @ApiResponse(responseCode = "200", description = "프로필 이미지 수정 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json"))
    public ResponseEntity<?> updateProfileImage(@RequestHeader("Authorization") String token,
                                                @RequestBody @Valid UserRequest.UpdateProfileImage request,
                                                BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            Long userId = jwtUtil.extractUserId(token);
            UserResponse.UpdatedProfileImage response = userService.updateProfileImage(userId, request);
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 이름 수정
    @PutMapping("/name")
    @Operation(summary = "사용자 이름 수정 API")
    @ApiResponse(responseCode = "200", description = "사용자 이름 수정 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json"))
    public ResponseEntity<?> updateName(@RequestHeader("Authorization") String token,
                                        @RequestBody @Valid UserRequest.UpdateName request,
                                        BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            Long userId = jwtUtil.extractUserId(token);
            UserResponse.UpdatedName response = userService.updateName(userId, request);
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 알림 설정 변경
    @PutMapping("/notification")
    @Operation(summary = "알림 설정 변경 API")
    @ApiResponse(responseCode = "200", description = "알림 설정 변경 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json"))
    public ResponseEntity<?> updateNotification(@RequestHeader("Authorization") String token,
                                                @RequestBody @Valid UserRequest.UpdateNotification request,
                                                BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(errors);
        }

        if (request.getNotificationEnabled() == null) {
            return ResponseEntity.badRequest().body("알림 설정은 true 또는 false만 가능합니다.");
        }

        try {
            Long userId = jwtUtil.extractUserId(token);
            UserResponse.UpdatedNotification response = userService.updateNotification(userId, request);
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/check")
    @Operation(summary = "출석 체크 API")
    public ResponseEntity<?> attendanceCheck(@RequestHeader("Authorization") String token){
        Long userId = jwtUtil.extractUserId(token);

        userService.attendanceCheck(userId);

        return ResponseEntity.ok().build();
    }


}

