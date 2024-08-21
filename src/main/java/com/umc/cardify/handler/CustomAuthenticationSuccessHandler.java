package com.umc.cardify.handler;

import com.umc.cardify.domain.User;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    // private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    String tokenUri;

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    String userInfoUri;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    String redirectUri;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    public MultiValueMap<String, String> accessTokenParams(String grantType, String clientId, String code, String redirect_uri) {
        MultiValueMap<String, String> accessTokenParams = new LinkedMultiValueMap<>();
        accessTokenParams.add("grant_type", grantType);
        accessTokenParams.add("client_id", clientId);
        accessTokenParams.add("code", code);
        accessTokenParams.add("redirect_uri", redirect_uri);
        return accessTokenParams;
    }

    // oauth2.0 성공시
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        log.info("onAuthenticationSuccess is being invoked");

        Object principal = authentication.getPrincipal();
        UserResponse.tokenInfo tokenInfo;
        String jwtToken;

        if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;

            // 카카오 로그인에서 필요한 사용자 정보 추출
            String email = (String) oAuth2User.getAttributes().get("kakao_account.email");
            String nickname = (String) oAuth2User.getAttributes().get("properties.nickname");

            // DB에서 사용자 검색 또는 새 사용자 생성
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setName(nickname);
                        newUser.setKakao(true);
                        newUser.setPassword(""); // 소셜 로그인 시 비밀번호는 필요 없음
                        return userRepository.save(newUser);
                    });

            Long userId = user.getUserId();
            tokenInfo = jwtUtil.generateTokens(userId);
            jwtToken = tokenInfo.getAccessToken();
        } else if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            String email = userDetails.getUsername();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Long userId = user.getUserId();
            tokenInfo = jwtUtil.generateTokens(userId);
            jwtToken = tokenInfo.getAccessToken();

//            String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/dashboard")
//                    .build()
//                    .encode(StandardCharsets.UTF_8)
//                    .toUriString();
        } else {
            throw new RuntimeException("Unknown principal type: " + principal.getClass().getName());
        }

        // 토큰 전달
        response.addCookie(createCookie("Authorization", jwtToken));
        response.setHeader("Authorization", "Bearer " + jwtToken);

        log.info("JWT Token generated: {}", jwtToken);
        String targetUrl = UriComponentsBuilder.fromUriString("https://cardify.co.kr/dashboard")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        // 로그인 성공 후 리다이렉트 (필요에 따라 URL 변경 가능)
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60 * 60 * 60);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }
}
