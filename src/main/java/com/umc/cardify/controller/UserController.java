package com.umc.cardify.controller;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.dto.user.UserRequest;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// if 문 빼기, try-catch 도 가능하면 optional 로 대체 
@Tag(name = "UserController", description = "유저 프로필 관련 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 로그아웃
    @PostMapping("/logout")
    @Operation(summary = "로그아웃 API")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    public ResponseEntity<UserResponse.LogoutResponse> logout(@RequestHeader("Authorization") String token) {
        try {
            userService.logout(token);
            return ResponseEntity.ok(new UserResponse.LogoutResponse("로그아웃 되었습니다.", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new UserResponse.LogoutResponse(e.getMessage(), false));
        }
    }

    // 마이페이지 조회
    @GetMapping("/mypage")
    @Operation(summary = "마이페이지 정보 조회 API")
    public ResponseEntity<UserResponse.MyPageInfo> getMyPageInfo(@RequestHeader("Authorization") String token) {
        UserResponse.MyPageInfo myPageInfo = userService.getMyPageInfo(token);
        return ResponseEntity.ok(myPageInfo);
    }

    // 프로필 이미지 수정
    @PutMapping("/profile-image")
    @Operation(summary = "프로필 이미지 수정 API")
    public ResponseEntity<?> updateProfileImage(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UserRequest.UpdateProfileImage request) {

        UserResponse.UpdatedProfileImage response = userService.updateProfileImage(token, request);
        return ResponseEntity.ok(response);
    }

    // 이름 수정
    @PutMapping("/name")
    @Operation(summary = "사용자 이름 수정 API")
    public ResponseEntity<?> updateName(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UserRequest.UpdateName request) {

        UserResponse.UpdatedName response = userService.updateName(token, request);
        return ResponseEntity.ok(response);
    }

    // 알림 설정 변경
    @PutMapping("/notification")
    @Operation(summary = "알림 설정 변경 API")
    public ResponseEntity<?> updateNotification(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UserRequest.UpdateNotification request) {

        UserResponse.UpdatedNotification response = userService.updateNotification(token, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check")
    @Operation(summary = "출석 체크 API")
    public ResponseEntity<?> attendanceCheck(@RequestHeader("Authorization") String token) {

        userService.attendanceCheck(token);
        return ResponseEntity.ok().build();
    }
}