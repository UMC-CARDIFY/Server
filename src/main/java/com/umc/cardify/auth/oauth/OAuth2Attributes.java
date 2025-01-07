package com.umc.cardify.auth.oauth;

import com.umc.cardify.domain.enums.AuthProvider;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Getter
@Builder
public class OAuth2Attributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String profileImage;
    private String providerId;

    public static OAuth2Attributes of(AuthProvider provider, Map<String, Object> attributes) {
        switch (provider) {
            case GOOGLE:
                return ofGoogle(attributes);
            case KAKAO:
                return ofKakao(attributes);
            default:
                throw new IllegalArgumentException("Invalid Provider Type.");
        }
    }

    private static OAuth2Attributes ofGoogle(Map<String, Object> attributes) {
        log.debug("Google attributes: {}", attributes);  // 받아온 속성 확인
        return OAuth2Attributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .profileImage((String) attributes.get("picture"))
                .providerId((String) attributes.get("sub"))
                .attributes(attributes)
                .nameAttributeKey("sub")
                .build();
    }

    private static OAuth2Attributes ofKakao(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuth2Attributes.builder()
                .name((String) kakaoProfile.get("nickname"))
                .email((String) kakaoAccount.get("email"))
                .profileImage((String) kakaoProfile.get("profile_image_url"))
                .providerId(String.valueOf(attributes.get("id")))
                .attributes(attributes)
                .nameAttributeKey("id")
                .build();
    }
}