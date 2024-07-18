package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.domain.User;
import com.umc.cardify.dto.user.UserRequest;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.umc.cardify.config.exception.ErrorResponseStatus.INVALID_PWD;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final JwtUtil jwtUtil;

    // 회원가입
    @Transactional
    public String registerUser(UserRequest.signIn request) {

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .kakao(false)
                .build();

        User result = userRepository.save(user);

        return result.getName();
    }

    // 로그인
    @Transactional
    public UserResponse.tokenInfo login(UserRequest.login request) {
        String email = request.getEmail();
        User user = userRepository.findByEmail(email);

        if (!user.getPassword().equals(request.getPassword())) {
            throw new BadRequestException(INVALID_PWD);
        }

        return jwtUtil.generateTokens(user.getUserId());
    }
}
