package com.umc.cardify.handler;

import com.umc.cardify.domain.User;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        log.info("onAuthenticationSuccess is being invoked");
        Object principal = authentication.getPrincipal();

        UserResponse.tokenInfo tokenInfo;
        String jwtToken;

        if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;
            String email = (String) oAuth2User.getAttributes().get("email");

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setName((String) oAuth2User.getAttributes().get("nickname"));
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
        } else {
            throw new RuntimeException("Unknown principal type: " + principal.getClass().getName());
        }

        // JWT 토큰을 쿠키에 저장하거나 클라이언트 측에 전달
        response.setHeader("Authorization", "Bearer " + jwtToken);

        // 로그인 성공 후 리다이렉트 (404 에러 뜨는 거 당연함)
        getRedirectStrategy().sendRedirect(request, response, "/auth/oauth-response/" + jwtToken);
    }
}
