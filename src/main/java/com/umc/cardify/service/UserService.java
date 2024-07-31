package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.domain.User;
import com.umc.cardify.dto.user.UserRequest;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.umc.cardify.config.exception.ErrorResponseStatus.DUPLICATE_ERROR;
import static com.umc.cardify.config.exception.ErrorResponseStatus.INVALID_PWD;

// 이메일 로그인 및 회원가입
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // 이메일 회원가입
    @Transactional
    public String registerUser(UserRequest.signUp request) {

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(encodedPassword)
                .kakao(false)
                .build();

        try {
            User result = userRepository.save(user);
            return result.getName();
        } catch (DuplicateKeyException e) {  // 중복 검사
            throw new BadRequestException(DUPLICATE_ERROR);
        } catch (Exception e) {
            // 예외 로그 남기기
            e.printStackTrace();
            // 원인 예외 접근
            Throwable cause = e.getCause();
            if (cause != null) {
                cause.printStackTrace();
            }
            throw new RuntimeException("Unexpected error occurred", e);
        }

    }

    // 이메일 로그인
    @Transactional
    public UserResponse.tokenInfo login(UserRequest.login request) {
        String email = request.getEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(INVALID_PWD));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException(INVALID_PWD);
        }

        return jwtUtil.generateTokens(user.getUserId());
    }


    // 로그아웃
    @Transactional
    public void logout() {
        // 로그아웃 처리 로직
    }

    // 카카오 로그인
    @Transactional
    public UserResponse.tokenInfo kakaoLogin(String code) {
        // 카카오 로그인 처리 로직
        // 카카오 API를 호출하여 사용자 정보를 가져오고, 사용자 인증 및 토큰 생성
        return UserResponse.tokenInfo.builder()
                .grantType("Bearer")
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .build();
        // return new UserResponse.tokenInfo("Bearer", "accessToken", "refreshToken");
    }

    // 토큰 갱신(얜 왜 있는 거지)
    @Transactional
    public UserResponse.tokenInfo refreshToken(String refreshToken) {
        String accessToken = jwtUtil.refreshAccessToken(refreshToken);

        return UserResponse.tokenInfo.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 사용자 등록
    public User registerUser(String email, String name) {
        User user = User.builder()
                .email(email)
                .name(name)
                .password(passwordEncoder.encode("default_password")) // OAuth2 사용자는 기본 비밀번호 사용
                .kakao(true)
                .build();

        return userRepository.save(user);
    }

    // 사용자 등록 후 JWT 생성
    public UserResponse.tokenInfo createToken(User user) {
        // JWT 생성
        String accessToken = jwtUtil.createAccessToken(user.getUserId());
        String refreshToken = jwtUtil.createRefreshToken(user.getUserId());

        return UserResponse.tokenInfo.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
