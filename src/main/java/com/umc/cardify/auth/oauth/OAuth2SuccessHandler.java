package com.umc.cardify.auth.oauth;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.AuthProvider;
import com.umc.cardify.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${spring.profiles.active:local}")  // 프로필 설정 추가 (로컬 환경)
    private String activeProfile;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // OAuth2 제공자 확인
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        // 사용자 정보 추출
        String email;
        String name;
        String profileImage;

        if (provider == AuthProvider.KAKAO) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            email = (String) kakaoAccount.get("email");
            name = (String) profile.get("nickname");
            profileImage = (String) profile.get("profile_image_url");
        } else { // GOOGLE
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
            profileImage = oAuth2User.getAttribute("picture");
        }

        // 사용자 정보 저장 또는 업데이트
        User user = userService.processSocialLogin(email, name, profileImage, provider);

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(email, provider);
        String refreshToken = jwtTokenProvider.createRefreshToken();

        // 토큰 정보 로깅
        log.info("Access Token: {}", accessToken);
        log.info("Refresh Token: {}", refreshToken);

        // 리프레시 토큰 저장
        user.setRefreshToken(refreshToken);
        userService.saveUser(user);

        // accessToken을 세션에 저장
        request.getSession().setAttribute("OAUTH2_ACCESS_TOKEN", accessToken);

        // refreshToken은 HttpOnly 쿠키에 저장
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(!activeProfile.equals("local"));
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(604800); // 7일
        response.addCookie(refreshTokenCookie);

        // 클린 URL로 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }
}