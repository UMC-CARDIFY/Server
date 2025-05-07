package com.umc.cardify.controller;

import com.umc.cardify.dto.payment.billing.BillingKeyRequestDTO;
import com.umc.cardify.dto.payment.billing.BillingKeyResponse;
import com.umc.cardify.dto.payment.subscription.SubscriptionRequest;
import com.umc.cardify.dto.payment.webhook.WebhookRequest;
import com.umc.cardify.service.payment.KakaoPaymentServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/kakaopay")
@RequiredArgsConstructor
@Tag(name = "KakaoPayController", description = "카카오페이 정기결제 관련 API")
public class KakaoPayController {

  private final KakaoPaymentServiceImpl kakaoPaymentServiceImpl;

  @Operation(summary = "빌링키 발급 요청", description = "카카오페이 정기결제를 위한 빌링키 발급을 요청합니다.")
  @PostMapping("/billing-key")
  public ResponseEntity<BillingKeyResponse.RequestBillingKeyRes> requestBillingKey(
      @Valid @RequestBody BillingKeyRequestDTO.RequestBillingKeyReq request) {

    log.info("카카오페이 빌링키 발급 요청: userId={}, productId={}", request.userId(), request.productId());
    try {
      BillingKeyResponse.RequestBillingKeyRes response = kakaoPaymentServiceImpl.requestBillingKey(request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("카카오페이 빌링키 발급 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @Operation(summary = "빌링키 검증", description = "발급된 빌링키를 검증하고 저장합니다.")
  @PostMapping("/billing-key/verify")
  public ResponseEntity<?> verifyAndSaveBillingKey(
      @Valid @RequestBody BillingKeyRequestDTO.VerifyBillingKeyReq request) {

    log.info("카카오페이 빌링키 검증 요청: customerUid={}, userId={}", request.customerUid(), request.userId());
    try {
      // 기존 메소드 사용
      BillingKeyResponse.VerifyBillingKeyRes response = kakaoPaymentServiceImpl.verifyAndSaveBillingKey(request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("카카오페이 빌링키 검증 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("빌링키 검증 중 오류 발생: " + e.getMessage());
    }
  }

  // 빌링키 발급 성공
  @GetMapping("/success")
  public ResponseEntity<?> billingKeySuccess(
      @RequestParam("pg_token") String pgToken,
      @RequestParam("merchant_uid") String merchantUid,
      @RequestParam(value = "customer_uid", required = false) String customerUid,
      @RequestParam(value = "tid", required = false) String tid) {

    log.info("빌링키 발급 성공 콜백: pgToken={}, merchantUid={}, customerUid={}, tid={}",
        pgToken, merchantUid, customerUid, tid);

    try {
      // 빌링키 발급 승인 처리
      BillingKeyResponse.ApproveBillingKeyRes response =
          kakaoPaymentServiceImpl.approveBillingKey(BillingKeyRequestDTO.ApproveBillingKeyReq.builder()
              .pgToken(pgToken)
              .merchantUid(merchantUid)
              .customerUid(customerUid)
              .tid(tid)
              .build());

      // 프론트엔드에서 처리할 수 있도록 결과 페이지로 리다이렉트
      // 실제 구현에서는 프론트엔드 URL을 사용해야 함
      return ResponseEntity.status(HttpStatus.FOUND)
          .header("Location", "/billing-success?merchant_uid=" + merchantUid)
          .build();
    } catch (Exception e) {
      log.error("빌링키 승인 처리 오류: {}", e.getMessage(), e);

      // 오류 발생 시 오류 페이지로 리다이렉트
      return ResponseEntity.status(HttpStatus.FOUND)
          .header("Location", "/billing-fail?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8))
          .build();
    }
  }

  // 빌링키 발급 실패 콜백 처리
  @GetMapping("/fail")
  public ResponseEntity<?> billingKeyFail(
      @RequestParam(value = "error_msg", required = false) String errorMsg,
      @RequestParam(value = "merchant_uid", required = false) String merchantUid) {

    log.error("빌링키 발급 실패: merchantUid={}, errorMsg={}", merchantUid, errorMsg);

    // 실패 페이지로 리다이렉트
    return ResponseEntity.status(HttpStatus.FOUND)
        .header("Location", "/billing-fail?error=" +
            URLEncoder.encode(errorMsg != null ? errorMsg : "빌링키 발급에 실패했습니다.", StandardCharsets.UTF_8))
        .build();
  }

  // 빌링키 승인 결과 확인 API (프론트엔드에서 호출)
  @GetMapping("/billing-status/{merchantUid}")
  public ResponseEntity<?> getBillingStatus(@PathVariable String merchantUid) {
    try {
      BillingKeyResponse.BillingStatusRes status = kakaoPaymentServiceImpl.getBillingStatus(merchantUid);
      return ResponseEntity.ok(status);
    } catch (Exception e) {
      log.error("빌링키 상태 조회 오류: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/test-redirect")
  public ResponseEntity<?> testRedirect(
      @RequestParam("merchant_uid") String merchantUid,
      @RequestParam("customer_uid") String customerUid) {

    // 테스트용 HTML 페이지 반환
    String html = "<!DOCTYPE html>"
        + "<html>"
        + "<head>"
        + "<title>카카오페이 테스트</title>"
        + "<meta charset=\"UTF-8\">"  // UTF-8 인코딩 명시
        + "</head>"
        + "<body>"
        + "<h1>카카오페이 빌링키 발급 시뮬레이션</h1>"
        + "<p>merchant_uid: " + merchantUid + "</p>"
        + "<p>customer_uid: " + customerUid + "</p>"
        + "<form action='/api/v1/payments/kakaopay/success' method='get'>"
        + "<input type='hidden' name='pg_token' value='test_pg_token' />"
        + "<input type='hidden' name='merchant_uid' value='" + merchantUid + "' />"
        + "<input type='hidden' name='customer_uid' value='" + customerUid + "' />"
        + "<button type='submit'>결제 성공</button>"
        + "</form>"
        + "<form action='/api/v1/payments/kakaopay/fail' method='get'>"
        + "<input type='hidden' name='merchant_uid' value='" + merchantUid + "' />"
        + "<input type='hidden' name='error_msg' value='사용자 취소' />"
        + "<button type='submit'>결제 취소</button>"
        + "</form>"
        + "</body>"
        + "</html>";

    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .header("Content-Type", "text/html; charset=UTF-8")  // 헤더에도 UTF-8 명시
        .body(html);
  }

  @Operation(summary = "결제 취소", description = "카카오페이 결제를 취소합니다.")
  @PostMapping("/payment/cancel")
  public ResponseEntity<Void> cancelPayment(
      @Valid @RequestBody SubscriptionRequest.CancelPaymentReq request) {

    log.info("카카오페이 결제 취소 요청: merchantUid={}", request.merchantUid());
    try {
      boolean success = kakaoPaymentServiceImpl.cancelPayment(request);
      return success
          ? ResponseEntity.ok().build()
          : ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    } catch (Exception e) {
      log.error("카카오페이 결제 취소 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Operation(summary = "웹훅 처리", description = "카카오페이/포트원에서 전송하는 결제 웹훅을 처리합니다.")
  @PostMapping("/webhook")
  public ResponseEntity<Void> handleWebhook(@RequestBody WebhookRequest request) {

    log.info("카카오페이 웹훅 수신: impUid={}, merchantUid={}, status={}",
        request.getImp_uid(), request.getMerchant_uid(), request.getStatus());
    try {
      kakaoPaymentServiceImpl.handleWebhook(request);
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      log.error("카카오페이 웹훅 처리 오류: {}", e.getMessage());
      // 웹훅은 항상 200 OK 응답 (재시도 방지)
      return ResponseEntity.ok().build();
    }
  }
}