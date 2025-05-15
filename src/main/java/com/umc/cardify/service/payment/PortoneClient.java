package com.umc.cardify.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.dto.payment.webhook.WebhookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortoneClient {

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${portone.imp_key}")
  private String apiKey;

  @Value("${portone.imp_secret}")
  private String apiSecret;

  private String cachedV2Token;
  private LocalDateTime v2TokenExpiresAt;

  private static final String PORTONE_API_URL = "https://api.iamport.kr";

  // 토큰 캐싱을 위한 필드 추가
  private String cachedToken;
  private LocalDateTime tokenExpiresAt;

  // 포트원 API 인증 토큰 발급 (캐싱 기능 추가)
  public String getAccessToken() {
    // 토큰이 있고 아직 유효한 경우 캐시된 토큰 반환
    if (cachedToken != null && tokenExpiresAt != null && LocalDateTime.now().plusMinutes(5).isBefore(tokenExpiresAt)) {
      log.debug("캐시된 포트원 토큰 사용: {}", maskToken(cachedToken));
      return cachedToken;
    }

    // 토큰이 없거나 만료 임박한 경우 새로 발급
    log.info("포트원 토큰 새로 발급 시작");
    String url = PORTONE_API_URL + "/users/getToken";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // API 키 로깅 (길이만)
    log.debug("API 키 정보: imp_key 길이={}, imp_secret 길이={}",
        apiKey != null ? apiKey.length() : 0,
        apiSecret != null ? apiSecret.length() : 0);

    try {
      // Map 대신 직접 JSON 문자열 생성 (이 부분이 문제였을 수 있음)
      String jsonBody = String.format("{\"imp_key\":\"%s\",\"imp_secret\":\"%s\"}",
          apiKey, apiSecret);

      HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

      log.debug("포트원 토큰 요청 본문: {}", jsonBody);

      // exchange 메서드 사용 (postForEntity 대신)
      ResponseEntity<String> response = restTemplate.exchange(
          url, HttpMethod.POST, request, String.class);

      log.debug("포트원 응답 상태 코드: {}", response.getStatusCodeValue());

      if (response.getBody() == null) {
        log.error("포트원 응답 본문이 없습니다");
        throw new RuntimeException("포트원 응답 본문이 없습니다");
      }

      JsonNode rootNode = objectMapper.readTree(response.getBody());
      log.debug("포트원 응답: {}", rootNode);

      if (rootNode.path("code").asInt() == 0) {
        // 토큰 추출 및 캐싱
        this.cachedToken = rootNode.path("response").path("access_token").asText();

        // 만료 시간 설정 (포트원 토큰은 약 30분간 유효)
        long expiresIn = rootNode.path("response").path("expired_at").asLong() -
            rootNode.path("response").path("now").asLong();
        this.tokenExpiresAt = LocalDateTime.now().plusSeconds(expiresIn);

        log.info("포트원 토큰 발급 성공 (만료: {})", tokenExpiresAt);
        return cachedToken;
      } else {
        log.error("포트원 토큰 발급 실패: {}", rootNode.path("message").asText());
        throw new RuntimeException("포트원 인증 실패: " + rootNode.path("message").asText());
      }
    } catch (HttpClientErrorException e) {
      log.error("포트원 API 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new RuntimeException("포트원 토큰 발급 중 오류: " + e.getResponseBodyAsString(), e);
    } catch (Exception e) {
      log.error("포트원 토큰 발급 중 오류: {}", e.getMessage(), e);
      throw new RuntimeException("포트원 토큰 발급 중 오류 발생", e);
    }
  }

  // 인증 헤더 생성 유틸리티 메소드
  private HttpHeaders getAuthHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", getAccessToken());  // Bearer 접두사 제거 (API에 따라 다를 수 있음)
    return headers;
  }

  // 토큰 마스킹 유틸리티
  private String maskToken(String token) {
    if (token == null || token.length() < 10) return "[masked]";
    return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
  }

  // 빌링키 조회
  public JsonNode getBillingKey(String customerUid) {
    log.debug("빌링키 조회 시작: customerUid={}", customerUid);
    String url = PORTONE_API_URL + "/subscribe/customers/" + customerUid;

    HttpEntity<Void> request = new HttpEntity<>(getAuthHeaders());

    try {
      ResponseEntity<String> response = restTemplate.exchange(
          url, HttpMethod.GET, request, String.class);

      if (response.getBody() == null) {
        log.error("빌링키 조회 응답 본문이 없습니다");
        throw new RuntimeException("빌링키 조회 응답 본문이 없습니다");
      }

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

  // 나머지 메서드들은 동일하게 유지...
  // 결제 정보 조회
  public JsonNode getPaymentData(String impUid) {
    log.debug("결제 정보 조회 시작: impUid={}", impUid);
    String url = PORTONE_API_URL + "/payments/" + impUid;

    HttpEntity<Void> request = new HttpEntity<>(getAuthHeaders());

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

  // 정기 결제 요청
  public JsonNode requestSubscriptionPayment(String customerUid, String merchantUid,
                                             String name, int amount, Map<String, String> params) {
    log.debug("정기 결제 요청 시작: customerUid={}, merchantUid={}, amount={}",
        customerUid, merchantUid, amount);
    String url = PORTONE_API_URL + "/subscribe/payments/again";

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("customer_uid", customerUid);
    requestBody.put("merchant_uid", merchantUid);
    requestBody.put("name", name);
    requestBody.put("amount", amount);

    // 추가 파라미터 적용
    if (params != null && !params.isEmpty()) {
      params.forEach(requestBody::put);
    }

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, getAuthHeaders());

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

  // 결제 취소
  public JsonNode cancelPayment(String merchantUid, String reason) {
    log.debug("결제 취소 요청 시작: merchantUid={}, reason={}", merchantUid, reason);
    String url = PORTONE_API_URL + "/payments/cancel";

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("merchant_uid", merchantUid);
    if (reason != null && !reason.isEmpty()) {
      requestBody.put("reason", reason);
    }

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, getAuthHeaders());

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

  // 웹훅 검증
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

      log.debug("웹훅 검증 성공: impUid={}, merchantUid={}",
          webhookData.getImp_uid(), webhookData.getMerchant_uid());
      return true;
    } catch (Exception e) {
      log.error("웹훅 검증 중 오류: {}", e.getMessage());
      return false;
    }
  }
}