package com.umc.cardify.service.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.domain.User;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();

        try {
            System.out.println(new ObjectMapper().writeValueAsString(attributes));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String email = (String) ((Map<String, Object>) attributes.get("kakao_account")).get("email");
        String nickname = (String) ((Map<String, Object>) ((Map<String, Object>) attributes.get("kakao_account")).get("profile")).get("nickname");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(nickname);
                    newUser.setKakao(true);
                    newUser.setPassword(""); // 소셜 로그인 시 비밀번호는 필요 없음
                    return userRepository.save(newUser);
                });

        return oAuth2User;
    }
}
