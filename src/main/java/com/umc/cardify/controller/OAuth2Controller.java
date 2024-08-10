package com.umc.cardify.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.dto.user.KakaoToken;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "OAuth2Controller", description = "카카오 로그인 관련 API")
@RestController
@RequestMapping("/")
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

        ResponseEntity<String> response2 = rt2.exchange(
                userInfoUri,
                HttpMethod.GET,
                kakaoTokenRequest2,
                String.class
        );

        return response2.getBody();

//        try {
//
//            ResponseEntity<String> response2 = rt2.exchange(
//                    userInfoUri,
//                    HttpMethod.GET,
//                    kakaoTokenRequest2,
//                    String.class
//            );
//
//            return response2.getBody();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }
}
