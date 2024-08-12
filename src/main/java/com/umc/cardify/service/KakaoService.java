package com.umc.cardify.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.domain.User;
import com.umc.cardify.dto.user.KakaoToken;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KakaoService {

    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String userInfoUri;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserResponse.tokenInfo processKakaoLogin(String code) throws JsonProcessingException {
        try {
            // 카카오 토큰 요청
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);
            params.add("code", code);

            HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.exchange(tokenUri, HttpMethod.POST, kakaoTokenRequest, String.class);

            // 토큰 파싱
            KakaoToken kakaoToken = objectMapper.readValue(response.getBody(), KakaoToken.class);

            // 사용자 정보 요청
            HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(createHeaders(kakaoToken.getAccess_token()));
            ResponseEntity<String> response2 = restTemplate.exchange(userInfoUri, HttpMethod.GET, kakaoUserInfoRequest, String.class);

            // 사용자 정보 파싱
            Map<String, Object> userInfo = objectMapper.readValue(response2.getBody(), HashMap.class);
            Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
            String email = (String) kakaoAccount.get("email");

            Map<String, Object> properties = (Map<String, Object>) userInfo.get("properties");
            String nickname = (String) properties.get("nickname");

            // 사용자 처리 및 JWT 생성
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setName(nickname);
                        newUser.setKakao(true);
                        newUser.setPassword(UUID.randomUUID().toString()); // 랜덤 비밀번호 설정
                        return userRepository.save(newUser);
                    });

            Long userId = user.getUserId();
            return jwtUtil.generateTokens(userId);

        } catch (HttpClientErrorException e) {
            e.printStackTrace();
            throw new RuntimeException("카카오 로그인 실패: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("카카오 로그인 실패: " + e.getMessage());
        }
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        return headers;
    }
}
