package com.umc.cardify.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.dto.payment.webhook.WebhookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortoneClient {

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${portone.api-key}")
  private String apiKey;

  @Value("${portone.api-secret}")
  private String apiSecret;

  private static final String PORTONE_API_URL = "https://api.iamport.kr";

  /**
   * 포트원 API 인증 토큰 발급
   */
  public String getAccessToken() {
    String url = PORTONE_API_URL + "/users/getToken";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("imp_key", apiKey);
    requestBody.put("imp_secret", apiSecret);

    HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

    try {
      ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
      JsonNode rootNode = objectMapper.readTree(response.getBody());

      if (rootNode.path("code").asInt() == 0) {
        String token = rootNode.path("response").path("access_token").asText();
        log.debug("포트원 토큰 발급 성공: {}", token.substring(0, 10) + "...");
        return token;
      } else {
        log.error("포트원 토큰 발급 실패: {}", rootNode.path("message").asText());
        throw new RuntimeException("포트원 인증 실패: " + rootNode.path("message").asText());
      }
    } catch (Exception e) {
      log.error("포트원 토큰 발급 중 오류: {}", e.getMessage());
      throw new RuntimeException("포트원 토큰 발급 중 오류 발생", e);
    }
  }

  /**
   * 빌링키 조회
   */
  public JsonNode getBillingKey(String customerUid) {
    log.debug("빌링키 조회 시작: customerUid={}", customerUid);
    String token = getAccessToken();
    String url = PORTONE_API_URL + "/subscribe/customers/" + customerUid;

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + token);

    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response = restTemplate.exchange(
          url, HttpMethod.GET, request, String.class);
      JsonNode rootNode = objectMapper.readTree(response.getBody());

      if (rootNode.path("code").asInt() == 0) {
        log.debug("빌링키 조회 성공: customerUid={}", customerUid);
        return rootNode.path("response");
      } else {
        log.error("빌링키 조회 실패: {}", rootNode.path("message").asText());
        throw new RuntimeException("빌링키 조회 실패: " + rootNode.path("message").asText());
      }
    } catch (Exception e) {
      log.error("빌링키 조회 중 오류: {}", e.getMessage());
      throw new RuntimeException("빌링키 조회 중 오류 발생", e);
    }
  }

  /**
   * 결제 정보 조회
   */
  public JsonNode getPaymentData(String impUid) {
    log.debug("결제 정보 조회 시작: impUid={}", impUid);
    String token = getAccessToken();
    String url = PORTONE_API_URL + "/payments/" + impUid;

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + token);

    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response = restTemplate.exchange(
          url, HttpMethod.GET, request, String.class);
      JsonNode rootNode = objectMapper.readTree(response.getBody());

      if (rootNode.path("code").asInt() == 0) {
        log.debug("결제 정보 조회 성공: impUid={}", impUid);
        return rootNode.path("response");
      } else {
        log.error("결제 정보 조회 실패: {}", rootNode.path("message").asText());
        throw new RuntimeException("결제 정보 조회 실패: " + rootNode.path("message").asText());
      }
    } catch (Exception e) {
      log.error("결제 정보 조회 중 오류: {}", e.getMessage());
      throw new RuntimeException("결제 정보 조회 중 오류 발생", e);
    }
  }

  /**
   * 정기 결제 요청
   */
  public JsonNode requestSubscriptionPayment(String customerUid, String merchantUid,
                                             String name, int amount, Map<String, String> params) {
    log.debug("정기 결제 요청 시작: customerUid={}, merchantUid={}, amount={}",
        customerUid, merchantUid, amount);
    String token = getAccessToken();
    String url = PORTONE_API_URL + "/subscribe/payments/again";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + token);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("customer_uid", customerUid);
    requestBody.put("merchant_uid", merchantUid);
    requestBody.put("name", name);
    requestBody.put("amount", amount);

    // 추가 파라미터 적용
    if (params != null && !params.isEmpty()) {
      params.forEach(requestBody::put);
    }

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

    try {
      ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
      JsonNode rootNode = objectMapper.readTree(response.getBody());

      if (rootNode.path("code").asInt() == 0) {
        log.debug("정기 결제 요청 성공: merchantUid={}", merchantUid);
        return rootNode.path("response");
      } else {
        log.error("정기 결제 요청 실패: {}", rootNode.path("message").asText());
        throw new RuntimeException("정기 결제 요청 실패: " + rootNode.path("message").asText());
      }
    } catch (Exception e) {
      log.error("정기 결제 요청 중 오류: {}", e.getMessage());
      throw new RuntimeException("정기 결제 요청 중 오류 발생", e);
    }
  }

  /**
   * 결제 취소
   */
  public JsonNode cancelPayment(String merchantUid, String reason) {
    log.debug("결제 취소 요청 시작: merchantUid={}, reason={}", merchantUid, reason);
    String token = getAccessToken();
    String url = PORTONE_API_URL + "/payments/cancel";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + token);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("merchant_uid", merchantUid);
    if (reason != null && !reason.isEmpty()) {
      requestBody.put("reason", reason);
    }

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

    try {
      ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
      JsonNode rootNode = objectMapper.readTree(response.getBody());

      if (rootNode.path("code").asInt() == 0) {
        log.debug("결제 취소 성공: merchantUid={}", merchantUid);
        return rootNode.path("response");
      } else {
        log.error("결제 취소 실패: {}", rootNode.path("message").asText());
        throw new RuntimeException("결제 취소 실패: " + rootNode.path("message").asText());
      }
    } catch (Exception e) {
      log.error("결제 취소 중 오류: {}", e.getMessage());
      throw new RuntimeException("결제 취소 중 오류 발생", e);
    }
  }

  /**
   * 웹훅 검증
   */
  public boolean verifyWebhook(WebhookRequest webhookData) {
    if (webhookData.getImp_uid() == null) {
      log.error("웹훅 검증 실패: imp_uid가 없습니다");
      return false;
    }

    try {
      JsonNode paymentData = getPaymentData(webhookData.getImp_uid());

      // 기본 검증: merchant_uid 일치 여부
      if (!paymentData.path("merchant_uid").asText().equals(webhookData.getMerchant_uid())) {
        log.error("웹훅 검증 실패: merchant_uid 불일치 (webhook={}, actual={})",
            webhookData.getMerchant_uid(), paymentData.path("merchant_uid").asText());
        return false;
      }

      // 필요시 추가 검증 로직 구현 가능
      // ex: 결제 금액 검증 등

      log.debug("웹훅 검증 성공: impUid={}, merchantUid={}",
          webhookData.getImp_uid(), webhookData.getMerchant_uid());
      return true;
    } catch (Exception e) {
      log.error("웹훅 검증 중 오류: {}", e.getMessage());
      return false;
    }
  }
}
