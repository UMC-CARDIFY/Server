package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.User;
import com.umc.cardify.dto.user.UserRequest;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 이메일 회원가입
    @Transactional
    public String signup(UserRequest.signUp request) {
        // 중복된 이메일 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException(ErrorResponseStatus.DUPLICATE_ERROR);
        }

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
    public UserResponse.tokenInfo login(UserRequest.login loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BadRequestException(ErrorResponseStatus.INVALID_PWD);
        }

        return jwtUtil.generateTokens(user.getUserId());
    }
}
