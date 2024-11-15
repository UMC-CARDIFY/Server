package com.umc.cardify.auth.oauth;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.AuthProvider;
import com.umc.cardify.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // OAuth2 제공자 확인
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        // OAuth2 사용자 정보 추출
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 카카오 응답에서 사용자 정보 추출
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String email = (String) kakaoAccount.get("email");
        String name = (String) profile.get("nickname");
        String profileImage = (String) profile.get("profile_image_url");

        // 사용자 정보 저장 또는 업데이트
        User user = userService.processSocialLogin(email, name, profileImage, provider);

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(email, provider);
        String refreshToken = jwtTokenProvider.createRefreshToken();

        // 리프레시 토큰 저장
        user.setRefreshToken(refreshToken);
        userService.saveUser(user);

        // 프론트엔드로 리다이렉트 (토큰과 함께)
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}