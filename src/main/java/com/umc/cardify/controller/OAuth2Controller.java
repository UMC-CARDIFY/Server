package com.umc.cardify.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.service.KakaoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "OAuth2Controller", description = "카카오 로그인 관련 API")
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final KakaoService kakaoService;

    @GetMapping("oauth2/callback/kakao")
    public @ResponseBody ResponseEntity<UserResponse.tokenInfo> kakaoCallback(@RequestParam String code) throws JsonProcessingException {
        UserResponse.tokenInfo tokenInfo = kakaoService.processKakaoLogin(code);
        return ResponseEntity.ok(tokenInfo); // 토큰 정보를 응답 바디에 포함시킴
    }


    @PostMapping("/kakao")
    public ResponseEntity<UserResponse.tokenInfo> kakaoLogin(@RequestParam String code) throws JsonProcessingException {
        UserResponse.tokenInfo tokenInfo = kakaoService.processKakaoLogin(code);
        return ResponseEntity.ok(tokenInfo);
    }


}
