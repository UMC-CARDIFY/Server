package com.umc.cardify.service;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.config.exception.ResourceNotFoundException;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.AuthProvider;
import com.umc.cardify.dto.user.UserRequest;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;  // JwtUtil 대신 JwtTokenProvider 사용

    // 로그아웃
    @Transactional
    public void logout(String token) {
        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", ""));

        User user = userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        if (user.getRefreshToken() == null) {
            throw new BadRequestException(ErrorResponseStatus.TOKEN_NOT_FOUND);
        }
        user.setRefreshToken(null);
        userRepository.save(user);
    }

    // 마이페이지 조회
    @Transactional(readOnly = true)
    public UserResponse.MyPageInfo getMyPageInfo(String token) {

        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
        User user = userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        return UserResponse.MyPageInfo.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .profileImage(user.getProfileImage())
                .point(user.getPoint())
                .notificationEnabled(user.isNotificationEnabled())
                .refreshToken(user.getRefreshToken())
                .todayCheck(user.getTodayCheck())
                .build();
    }

    // 프로필 이미지 수정
    @Transactional
    public UserResponse.UpdatedProfileImage updateProfileImage(String token, UserRequest.UpdateProfileImage request) {

        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
        User user = userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        user.setProfileImage(request.getProfileImage());
        User updatedUser = userRepository.save(user);

        return UserResponse.UpdatedProfileImage.builder()
                .profileImage(updatedUser.getProfileImage())
                .build();
    }

    // 이름 수정
    @Transactional
    public UserResponse.UpdatedName updateName(String token, UserRequest.UpdateName request) {

        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
        User user = userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        user.setName(request.getName());
        User updatedUser = userRepository.save(user);

        return UserResponse.UpdatedName.builder()
                .name(updatedUser.getName())
                .build();
    }

    // 알림 설정 변경
    @Transactional
    public UserResponse.UpdatedNotification updateNotification(String token, UserRequest.UpdateNotification request) {

        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
        User user = userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        user.setNotificationEnabled(request.getNotificationEnabled());
        User updatedUser = userRepository.save(user);

        return UserResponse.UpdatedNotification.builder()
                .notificationEnabled(updatedUser.isNotificationEnabled())
                .build();
    }

    @Transactional
    public void attendanceCheck(String token) {

        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
        User user = userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        user.setPoint(user.getPoint() + 100);
        user.setTodayCheck(1);
        userRepository.save(user);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void resetTodayCheck() {
        userRepository.resetAllTodayCheck();
    }
    // 토큰 갱신
    @Transactional
    public UserResponse.TokenInfo refreshToken(String refreshToken) {
        // 리프레시 토큰 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException(ErrorResponseStatus.INVALID_TOKEN);
        }

        // 리프레시 토큰으로 사용자 찾기
        User user = userRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.TOKEN_NOT_FOUND));

        // 새로운 액세스 토큰 발급 (이메일과 제공자 모두 포함)
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getProvider());
        String newRefreshToken = jwtTokenProvider.createRefreshToken();

        // 리프레시 토큰 업데이트
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        return UserResponse.TokenInfo.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .build();
    }

    // 소셜 로그인 성공 처리
    @Transactional
    public User processSocialLogin(String email, String name, String profileImage, AuthProvider provider) {
        return userRepository.findByEmailAndProvider(email, provider)
                .map(existingUser -> {
                    // 기존 사용자 정보 업데이트
                    existingUser.setName(name);
                    existingUser.setProfileImage(profileImage);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    // 새로운 사용자 생성
                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .profileImage(profileImage)
                            .provider(provider)
                            .point(5000)  // 초기 포인트
                            .notificationEnabled(true)  // 기본 알림 설정
                            .build();
                    return userRepository.save(newUser);
                });
    }

    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}