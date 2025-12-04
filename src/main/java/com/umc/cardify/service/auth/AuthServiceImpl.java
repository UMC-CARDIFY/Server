package com.umc.cardify.service.auth;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.domain.User;
import com.umc.cardify.dto.auth.AuthResponse;
import com.umc.cardify.config.exception.InvalidTokenException;
import com.umc.cardify.config.exception.TokenNotFoundException;
import com.umc.cardify.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final JwtTokenProvider tokenProvider;

  @Override
  public AuthResponse.RefreshTokenRes refreshAccessToken(HttpServletRequest request) {
    // 1. 쿠키에서 리프레시 토큰 추출
    String refreshToken = extractRefreshTokenFromCookie(request);

    if (refreshToken == null) {
      throw new TokenNotFoundException("리프레시 토큰이 없습니다.");
    }

    // 2. 리프레시 토큰 검증
    if (!tokenProvider.validateToken(refreshToken)) {
      throw new InvalidTokenException("유효하지 않은 리프레시 토큰입니다.");
    }

    // 3. 사용자 조회
    User user = userRepository.findByRefreshToken(refreshToken)
        .orElseThrow(() -> new InvalidTokenException("해당 리프레시 토큰의 사용자를 찾을 수 없습니다."));

    // 4. 새로운 액세스 토큰 발급
    String newAccessToken = tokenProvider.createAccessToken(
        user.getEmail(),
        user.getProvider()
    );

    log.info("액세스 토큰 갱신 성공: userId={}, email={}", user.getId(), user.getEmail());

    // 5. 응답 생성
    return AuthResponse.RefreshTokenRes.builder()
        .accessToken(newAccessToken)
        .build();
  }

  @Override
  public AuthResponse.CheckRefreshTokenRes checkRefreshToken(HttpServletRequest request) {
    // 1. 쿠키에서 리프레시 토큰 추출
    String refreshToken = extractRefreshTokenFromCookie(request);

    // 2. 토큰 유효성 검사
    boolean isValid = false;
    if (refreshToken != null) {
      try {
        isValid = tokenProvider.validateToken(refreshToken);
      } catch (Exception e) {
        log.warn("리프레시 토큰 검증 실패: {}", e.getMessage());
        isValid = false;
      }
    }

    // 3. 응답 생성
    return AuthResponse.CheckRefreshTokenRes.builder()
        .valid(isValid)
        .build();
  }

  /**
   * 쿠키에서 리프레시 토큰 추출
   */
  private String extractRefreshTokenFromCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();

    if (cookies == null) {
      return null;
    }

    for (Cookie cookie : cookies) {
      if ("refreshToken".equals(cookie.getName())) {
        return cookie.getValue();
      }
    }

    return null;
  }
}