package com.umc.cardify.controller;

import com.umc.cardify.dto.payment.billing.BillingKeyRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/kakaopay")
@RequiredArgsConstructor
@Tag(name = "카카오페이 결제 API", description = "카카오페이 정기결제 관련 API")
public class KakaoPayController {

  private final KakaoPaymentServiceImpl paymentService;

  @Operation(summary = "빌링키 발급 요청", description = "카카오페이 정기결제를 위한 빌링키 발급을 요청합니다.")
  @PostMapping("/billing-key")
  public ResponseEntity<BillingKeyResponse.RequestBillingKeyRes> requestBillingKey(
      @Valid @RequestBody BillingKeyRequest.RequestBillingKeyReq request) {

    log.info("카카오페이 빌링키 발급 요청: userId={}, productId={}", request.userId(), request.productId());
    try {
      BillingKeyResponse.RequestBillingKeyRes response = paymentService.requestBillingKey(request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("카카오페이 빌링키 발급 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @Operation(summary = "빌링키 검증", description = "발급된 빌링키를 검증하고 저장합니다.")
  @PostMapping("/billing-key/verify")
  public ResponseEntity<?> verifyAndSaveBillingKey(
      @Valid @RequestBody BillingKeyRequest.VerifyBillingKeyReq request) {

    log.info("카카오페이 빌링키 검증 요청: customerUid={}, userId={}", request.customerUid(), request.userId());
    try {
      // 기존 메소드 사용
      BillingKeyResponse.VerifyBillingKeyRes response = paymentService.verifyAndSaveBillingKey(request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("카카오페이 빌링키 검증 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("빌링키 검증 중 오류 발생: " + e.getMessage());
    }
  }

  @Operation(summary = "결제 취소", description = "카카오페이 결제를 취소합니다.")
  @PostMapping("/payment/cancel")
  public ResponseEntity<Void> cancelPayment(
      @Valid @RequestBody SubscriptionRequest.CancelPaymentReq request) {

    log.info("카카오페이 결제 취소 요청: merchantUid={}", request.merchantUid());
    try {
      boolean success = paymentService.cancelPayment(request);
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
      paymentService.handleWebhook(request);
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      log.error("카카오페이 웹훅 처리 오류: {}", e.getMessage());
      // 웹훅은 항상 200 OK 응답 (재시도 방지)
      return ResponseEntity.ok().build();
    }
  }
}