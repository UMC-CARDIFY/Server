package com.umc.cardify.service.security;

import com.umc.cardify.domain.User;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

// 카카오 로그인 처리
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    // 카카오 로그인 요청 처리, 사용자 정보 로드
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        User user = userRepository.findByEmail(email).orElseGet(()
                -> registerUser(attributes));

        return (OAuth2User) new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // 카카오 회원가입
    private User registerUser(Map<String, Object> attributes) {
        User user = User.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .password("") // 카카오 로그인 사용자에게는 비밀번호가 필요하지 않음
                .kakao(true)
                .build();
        return userRepository.save(user);
    }
}
