package com.umc.cardify.service;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
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

// 커스텀 익셉션 만들기
// return 문은 try 밖에서 하기
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;  // JwtUtil 대신 JwtTokenProvider 사용

    // 로그아웃
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));
        if (user.getRefreshToken() == null) {
            throw new BadRequestException(ErrorResponseStatus.TOKEN_NOT_FOUND);
        }
        user.setRefreshToken(null);
        userRepository.save(user);
    }

    // 마이페이지 정보 조회
    @Transactional(readOnly = true)
    public UserResponse.MyPageInfo getMyPageInfo(String email) {
        User user = userRepository.findByEmail(email)
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
    public UserResponse.UpdatedProfileImage updateProfileImage(String email, UserRequest.UpdateProfileImage request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        user.setProfileImage(request.getProfileImage());
        User updatedUser = userRepository.save(user);

        return UserResponse.UpdatedProfileImage.builder()
                .profileImage(updatedUser.getProfileImage())
                .build();
    }

    // 이름 수정
    @Transactional
    public UserResponse.UpdatedName updateName(String email, UserRequest.UpdateName request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        user.setName(request.getName());
        User updatedUser = userRepository.save(user);

        return UserResponse.UpdatedName.builder()
                .name(updatedUser.getName())
                .build();
    }

    // 알림 설정 변경
    @Transactional
    public UserResponse.UpdatedNotification updateNotification(String email, UserRequest.UpdateNotification request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        user.setNotificationEnabled(request.getNotificationEnabled());
        User updatedUser = userRepository.save(user);

        return UserResponse.UpdatedNotification.builder()
                .notificationEnabled(updatedUser.isNotificationEnabled())
                .build();
    }

    @Transactional
    public void attendanceCheck(String email) {
        User user = userRepository.findByEmail(email)
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

        // 새로운 액세스 토큰 발급
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

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            return jwtTokenProvider.validateToken(token);
        } catch (Exception e) {
            throw new BadRequestException(ErrorResponseStatus.INVALID_TOKEN);
        }
    }

    // 사용자 권한 검증
    public boolean validateUserAccess(String email, Long userId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        if (!user.getUserId().equals(userId)) {
            throw new BadRequestException(ErrorResponseStatus.INVALID_SOCIAL_LOGIN);
        }

        return true;
    }

    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}