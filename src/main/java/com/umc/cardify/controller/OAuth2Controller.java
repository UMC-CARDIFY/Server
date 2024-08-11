package com.umc.cardify.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.domain.User;
import com.umc.cardify.dto.user.KakaoToken;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "OAuth2Controller", description = "카카오 로그인 관련 API")
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class OAuth2Controller {

    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    String tokenUri;

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    String userInfoUri;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    String redirectUri;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    private final UserRepository userRepository;

    private final JwtUtil jwtUtil;
    @GetMapping("oauth2/callback/kakao")
    public @ResponseBody String kakaoCallback(String code) throws JsonProcessingException {

        // POST 방식으로 key=value 데이터를 요청 (카카오쪽으로)
        // 이 때 필요한 라이브러리가 RestTemplate, 얘를 쓰면 http 요청을 편하게 할 수 있다.
        RestTemplate rt = new RestTemplate();

        ObjectMapper objectMapper = new ObjectMapper();

        // HTTP POST를 요청할 때 보내는 데이터(body)를 설명해주는 헤더도 만들어 같이 보내줘야 한다.
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        // body 데이터를 담을 오브젝트인 MultiValueMap를 만들어보자
        // body는 보통 key, value의 쌍으로 이루어지기 때문에 자바에서 제공해주는 MultiValueMap 타입을 사용한다.
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        // 요청하기 위해 헤더(Header)와 데이터(Body)를 합친다.
        // kakaoTokenRequest는 데이터(Body)와 헤더(Header)를 Entity가 된다.
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(params, headers);

//        try {
//            ResponseEntity<String> response = rt.exchange(
//                    tokenUri,
//                    HttpMethod.POST,
//                    kakaoTokenRequest,
//                    String.class
//            );
//            return "카카오 토큰 요청 완료 : 토큰 요청에 대한 응답 : " + response.getBody();
//        } catch (HttpClientErrorException e) {
//            // 401 Unauthorized 예외를 잡고 로그로 출력
//            e.printStackTrace();
//            return "카카오 토큰 요청 실패 : " + e.getStatusCode() + " " + e.getResponseBodyAsString();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "카카오 토큰 요청 실패 : " + e.getMessage();
//        }

        ResponseEntity<String> response = rt.exchange(
                tokenUri,
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
        );

        KakaoToken kakaoToken = objectMapper.readValue(response.getBody(), KakaoToken.class);
        System.out.println("카카오 액세스 토큰: " + kakaoToken.getAccess_token());

        // 회원 정보 요청
        RestTemplate rt2 = new RestTemplate();

        // HTTP POST를 요청할 때 보내는 데이터(body)를 설명해주는 헤더도 만들어 같이 보내줘야 한다.
        HttpHeaders headers2 = new HttpHeaders();
        headers2.add("Authorization", "Bearer " + kakaoToken.getAccess_token());
        headers2.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        // 요청하기 위해 헤더(Header)와 데이터(Body)를 합친다.
        // kakaoTokenRequest는 데이터(Body)와 헤더(Header)를 Entity가 된다.
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest2 = new HttpEntity<>(headers2);

        try {

            ResponseEntity<String> response2 = rt2.exchange(
                    userInfoUri,
                    HttpMethod.GET,
                    kakaoTokenRequest2,
                    String.class
            );

            // 사용자 정보 파싱 (필요한 데이터 추출)
            Map<String, Object> userInfo = objectMapper.readValue(response2.getBody(), HashMap.class);

            // 카카오 계정 정보 (kakao_account) 가져오기
            Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
            String email = (String) kakaoAccount.get("email");

            // 사용자 프로필 정보 (properties) 가져오기
            Map<String, Object> properties = (Map<String, Object>) userInfo.get("properties");
            String nickname = (String) properties.get("nickname");

            // DB에서 사용자 검색 또는 새 사용자 생성
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setName(nickname);
                        newUser.setKakao(true);
                        newUser.setPassword(""); // 소셜 로그인 시 비밀번호는 필요 없음
                        return userRepository.save(newUser);
                    });

            Long userId = user.getUserId();
            UserResponse.tokenInfo tokenInfo = jwtUtil.generateTokens(userId);
            String accessToken = tokenInfo.getAccessToken();

            return accessToken;
        } catch (Exception e) {
            e.printStackTrace();
        }


        return "카카오 로그인 실패";
    }
}
