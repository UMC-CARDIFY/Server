package com.umc.cardify.handler;

import com.umc.cardify.domain.User;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public CustomAuthenticationSuccessHandler(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // Authentication 객체에서 사용자 정보를 가져옴
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername(); // 이메일이 username으로 설정되어 있다고 가정

        // 이메일로 사용자 엔티티를 조회
        User user = userRepository.findByEmailAndKakao(email, true)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user email"));

        // JWT 토큰 생성
        UserResponse.tokenInfo tokenInfo = jwtUtil.generateTokens(user.getUserId());
        response.setHeader("Authorization", "Bearer " + tokenInfo.getAccessToken());
        response.setHeader("Refresh-Token", tokenInfo.getRefreshToken());

        System.out.println("카카오 로그인 토큰: " + tokenInfo.getAccessToken());

        // 로그인 성공 후 리디렉션 URL 설정
        response.sendRedirect("/home");
    }
}
