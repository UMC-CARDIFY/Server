//package com.umc.cardify.controller;
//
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.ResponseEntity;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.client.RestTemplate;
//import org.json.JSONObject;
//
//import java.util.Map;
//
//@Tag(name = "KakaoAuthController", description = "카카오 로그인 관련 API")
//@RestController
//@RequestMapping("/api/v1/auth")
//public class KakaoAuthController {
//
//    private final RestTemplate restTemplate;
//
//    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
//    String tokenUri;
//
//    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
//    String userInfoUri;
//
//    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
//    String clientId;
//
//    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
//    String redirectUri;
//
//    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
//    private String clientSecret;
//
//    public KakaoAuthController(RestTemplate restTemplate) {
//        this.restTemplate = restTemplate;
//    }
//
//    @PostMapping("/kakao")
//    public ResponseEntity<?> kakaoLogin(@RequestBody Map<String, String> payload) {
//        String code = payload.get("code");
//
//        // 1. 액세스 토큰 요청
//        String accessToken = getAccessToken(code);
//        if (accessToken == null) {
//            return ResponseEntity.status(400).body("카카오 로그인 실패: 액세스 토큰을 받을 수 없습니다.");
//        }
//
//        // 2. 액세스 토큰을 사용하여 사용자 정보 요청
//        String userInfo = getUserInfo(accessToken);
//        if (userInfo == null) {
//            return ResponseEntity.status(400).body("카카오 로그인 실패: 사용자 정보를 받을 수 없습니다.");
//        }
//
//        // 사용자 정보 반환 또는 처리 (예: 사용자 DB에 저장)
//        return ResponseEntity.ok(userInfo);
//    }
//
//    // 액세스 토큰 요청 메서드
//    private String getAccessToken(String code) {
//
//
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("grant_type", "authorization_code");
//        params.add("client_id", clientId);
//        params.add("redirect_uri", redirectUri);
//        params.add("code", code);
//        params.add("client_secret", clientSecret); // Client Secret 추가
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Content-Type", "application/x-www-form-urlencoded");
//
//        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
//
//        try {
//            ResponseEntity<String> response = restTemplate.postForEntity(tokenUri, request, String.class);
//            if (response.getStatusCode().is2xxSuccessful()) {
//                JSONObject jsonObject = new JSONObject(response.getBody());
//                return jsonObject.getString("access_token");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    // 사용자 정보 요청 메서드
//    private String getUserInfo(String accessToken) {
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Authorization", "Bearer " + accessToken);
//
//        HttpEntity<Void> request = new HttpEntity<>(headers);
//
//        try {
//            ResponseEntity<String> response = restTemplate.exchange(userInfoUri, HttpMethod.GET, request, String.class);
//            if (response.getStatusCode().is2xxSuccessful()) {
//                return response.getBody();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//}
