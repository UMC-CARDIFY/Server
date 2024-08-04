package com.umc.cardify.service.security;

import com.umc.cardify.domain.User;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {

        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);

            // Log the user attributes for debugging
            logger.debug("OAuth2 User Attributes: {}", oAuth2User.getAttributes());
            // Custom processing of the user information

            Map<String, Object> attributes = oAuth2User.getAttributes();

            // 카카오 사용자 정보 추출
            String email = (String) attributes.get("kakao_account.email");
            String name = (String) attributes.get("properties.nickname");

            // DB에서 사용자 조회 또는 신규 사용자 저장
            User user = userRepository.findByEmailAndKakao(email, true)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setName(name);
                        newUser.setKakao(true);
                        newUser.setPassword(""); // 비밀번호는 빈 문자열로 설정 (소셜 로그인 사용)
                        return userRepository.save(newUser);
                    });

            return new CustomOAuth2User(Collections.singleton(new OAuth2UserAuthority(attributes)), attributes, "id");

        } catch (OAuth2AuthenticationException e) {
            logger.error("OAuth2 Authentication Exception", e);
            throw e;
        }

    }
}