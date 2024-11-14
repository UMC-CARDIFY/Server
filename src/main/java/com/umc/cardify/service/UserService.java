package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.User;
import com.umc.cardify.dto.user.UserRequest;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.auth.jwt.JwtUtil;
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
                .profileImage(null)
                .notificationEnabled(true)
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

    // 로그아웃
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        // 리프레시 토큰 무효화
        invalidateRefreshToken(user);
    }

    private void invalidateRefreshToken(User user) {
        // 리프래시 토큰 null 로 설정해 무효화
        user.setRefreshToken(null);
        userRepository.save(user);
    }

    // 마이페이지 정보 조회
    @Transactional(readOnly = true)
    public UserResponse.MyPageInfo getMyPageInfo(Long userId) {
        User user = userRepository.findByUserId(userId)
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
    public UserResponse.UpdatedProfileImage updateProfileImage(Long userId, UserRequest.UpdateProfileImage request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        user.setProfileImage(request.getProfileImage());
        User updatedUser = userRepository.save(user);

        return UserResponse.UpdatedProfileImage.builder()
                .profileImage(updatedUser.getProfileImage())
                .build();
    }

    // 이름 수정
    @Transactional
    public UserResponse.UpdatedName updateName(Long userId, UserRequest.UpdateName request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        user.setName(request.getName());
        User updatedUser = userRepository.save(user);

        return UserResponse.UpdatedName.builder()
                .name(updatedUser.getName())
                .build();
    }

    // 알림 설정 변경
    @Transactional
    public UserResponse.UpdatedNotification updateNotification(Long userId, UserRequest.UpdateNotification request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        user.setNotificationEnabled(request.getNotificationEnabled());
        User updatedUser = userRepository.save(user);

        return UserResponse.UpdatedNotification.builder()
                .notificationEnabled(updatedUser.isNotificationEnabled())
                .build();
    }

    @Transactional
    public void attendanceCheck(Long userId){
        User user = userRepository.findByUserId(userId)
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
}
