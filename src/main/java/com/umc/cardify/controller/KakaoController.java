package com.umc.cardify.controller;

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
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("")
public class KakaoController {

    @GetMapping("/home")
    public String home() {
        return "welcome Home";
    }
    @GetMapping("/login")
    public String login() {
        return "login page";
    }

    @GetMapping("/loginFailure")
    public String loginFailure() {
        return "login failure";
    }

}
