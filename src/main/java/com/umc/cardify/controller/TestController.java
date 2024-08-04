package com.umc.cardify.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("")
public class TestController {

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
