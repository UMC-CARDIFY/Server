package com.umc.cardify.controller;

import com.umc.cardify.service.payment.PortoneClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

  private final PortoneClient portoneClient;

  public TestController(PortoneClient portoneClient) {
    this.portoneClient = portoneClient;
  }

  @GetMapping("/token")
  public ResponseEntity<Map<String, Object>> testToken() {
    try {
      String token = portoneClient.getAccessToken();
      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("token", token);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      Map<String, Object> response = new HashMap<>();
      response.put("success", false);
      response.put("error", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }
}
