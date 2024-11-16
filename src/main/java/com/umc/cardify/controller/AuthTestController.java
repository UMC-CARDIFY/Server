package com.umc.cardify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/test")
@Tag(name = "AuthTestController", description = "토큰 테스트용 API")
@RequiredArgsConstructor
public class AuthTestController {

    @GetMapping("/token")
    @Operation(summary = "현재 토큰 정보 확인")
    public ResponseEntity<Map<String, String>> getCurrentToken(@RequestHeader("Authorization") String token) {
        // Bearer 제거
        String actualToken = token.replace("Bearer ", "");

        Map<String, String> tokenInfo = new HashMap<>();
        tokenInfo.put("accessToken", actualToken);
        tokenInfo.put("tokenType", "Bearer");

        return ResponseEntity.ok(tokenInfo);
    }

    @GetMapping("/user-info")
    @Operation(summary = "현재 토큰의 사용자 정보 확인")
    public ResponseEntity<Map<String, Object>> getCurrentUserInfo(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");

        // JWT 토큰 디코딩
        String[] chunks = actualToken.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();

        String payload = new String(decoder.decode(chunks[1]));

        // JSON 파싱
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> claims = mapper.readValue(payload, Map.class);
            return ResponseEntity.ok(claims);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}