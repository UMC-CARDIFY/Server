package com.umc.cardify.auth.oauth;

import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.AuthProvider;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // OAuth2 공급자 (Google, Kakao 등) 확인
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        // 공급자별 OAuth2 속성 추출
        OAuth2Attributes attributes = OAuth2Attributes.of(provider, oauth2User.getAttributes());

        // 사용자 정보 저장 또는 업데이트
        User user = saveOrUpdateUser(attributes, provider);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }

    private User saveOrUpdateUser(OAuth2Attributes attributes, AuthProvider provider) {
        User user = userRepository.findByEmailAndProvider(attributes.getEmail(), provider)
                .map(existingUser -> {
                    existingUser.setName(attributes.getName());
                    existingUser.setProfileImage(attributes.getProfileImage());
                    return existingUser;
                })
                .orElse(User.builder()
                        .email(attributes.getEmail())
                        .name(attributes.getName())
                        .profileImage(attributes.getProfileImage())
                        .provider(provider)
                        .providerId(attributes.getProviderId())
                        .point(5000)
                        .build());

        return userRepository.save(user);
    }
}