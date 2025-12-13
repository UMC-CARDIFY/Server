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
import com.umc.cardify.config.OAuth2Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final OAuth2Properties oauth2Properties;

    @Value("${spring.profiles.active:local}")
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

        // 항상 새 토큰 발급
        log.info("로그인 감지 - 새 토큰 발급: {}", email);

        // 기존 리프레시 토큰 무효화 (DB에서)
        if (user.getRefreshToken() != null) {
            log.info("기존 리프레시 토큰 무효화: {}", email);
            user.setRefreshToken(null);
        }

        // 새 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(email, provider);
        String refreshToken = jwtTokenProvider.createRefreshToken();

        // 토큰 정보 로깅 (토큰 일부만 로깅)
        log.info("새 Access Token 생성: {}...{}",
            accessToken.substring(0, 10),
            accessToken.substring(accessToken.length() - 10));
        log.info("새 Refresh Token 생성: {}...{}",
            refreshToken.substring(0, 10),
            refreshToken.substring(refreshToken.length() - 10));

        // 리프레시 토큰 저장
        user.setRefreshToken(refreshToken);
        userService.saveUser(user);

        // refreshToken은 HttpOnly 쿠키에 저장
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(!activeProfile.equals("local"));
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(604800); // 7일
        response.addCookie(refreshTokenCookie);

        // Origin 헤더에서 요청 출처 확인
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");

        log.info("Origin: {}, Referer: {}", origin, referer);

        // redirect URI 선택
        String selectedRedirectUri = oauth2Properties.getRedirectUris().stream()
            .filter(uri -> {
                if (origin != null && uri.startsWith(origin)) {
                    return true;
                }
                if (referer != null && uri.startsWith(getBaseUrl(referer))) {
                    return true;
                }
                return false;
            })
            .findFirst()
            .orElse(oauth2Properties.getRedirectUris().get(0));

        // 쿼리 파라미터로 Access Token 전달
        String redirectUrl = selectedRedirectUri + "?accessToken=" + accessToken;

        log.info("🔀 리다이렉트 (토큰 포함): {}", selectedRedirectUri);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String getBaseUrl(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            return uri.getScheme() + "://" + uri.getHost() +
                (uri.getPort() != -1 ? ":" + uri.getPort() : "");
        } catch (Exception e) {
            return url;
        }
    }
}