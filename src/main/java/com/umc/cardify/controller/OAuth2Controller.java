package com.umc.cardify.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.service.KakaoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "OAuth2Controller", description = "카카오 로그인 관련 API")
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final KakaoService kakaoService;

    @GetMapping("oauth2/callback/kakao")
    public @ResponseBody ResponseEntity<?> kakaoCallback(String code) throws JsonProcessingException {
        try {
            UserResponse.tokenInfo tokenInfo = kakaoService.processKakaoLogin(code);
            return ResponseEntity.ok().body(tokenInfo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
