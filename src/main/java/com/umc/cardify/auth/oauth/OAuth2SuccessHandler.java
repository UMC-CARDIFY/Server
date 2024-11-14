package com.umc.cardify.auth.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.AuthProvider;
import com.umc.cardify.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 사용자 정보 추출
        String email = (String) oAuth2User.getAttributes().get("email");
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        // 토큰 생성
        String accessToken = tokenProvider.createAccessToken(email, provider);
        String refreshToken = tokenProvider.createRefreshToken();

        // Refresh Token DB 저장
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        // 응답 생성
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(tokens));
    }
}