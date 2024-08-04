package com.umc.cardify.controller;

import com.umc.cardify.dto.user.UserRequest;
import com.umc.cardify.dto.user.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "OAuth2Controller", description = "카카오 로그인 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class OAuth2Controller {

    // 컨트롤러는 이후에..
//    @GetMapping("/kakao")
//    @Operation(summary = "카카오 로그인 API")
//    // @ApiResponse(responseCode = "200", description = "회원가입 성공")
//    // @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json"))
//    public ResponseEntity<UserResponse.tokenInfo> login(@RequestParam("jwtToken") String jwtToken) {
//        System.out.println("jwt token: " + jwtToken);
//        //UserResponse.tokenInfo tokenInfo = userService.login(loginRequest);
//        return ResponseEntity.ok(jwtToken);
//    }
}
