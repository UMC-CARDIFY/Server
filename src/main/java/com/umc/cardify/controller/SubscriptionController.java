package com.umc.cardify.controller;

import com.umc.cardify.config.exception.ResourceNotFoundException;
import com.umc.cardify.dto.payment.subscription.SubscriptionRequest;
import com.umc.cardify.dto.payment.subscription.SubscriptionResponse;
import com.umc.cardify.dto.payment.subscription.SubscriptionResponse.SubscriptionInfoRes;
import com.umc.cardify.service.payment.PaymentService;
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
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "구독 API", description = "구독 관련 API")
public class SubscriptionController {

  private final PaymentService paymentService;

  @Operation(summary = "구독 생성", description = "새로운 구독을 생성합니다.")
  @PostMapping
  public ResponseEntity<SubscriptionInfoRes> createSubscription(
      @Valid @RequestBody SubscriptionRequest.CreateSubscriptionReq request) {

    log.info("구독 생성 요청: userId={}, productId={}", request.userId(), request.productId());
    try {
      SubscriptionInfoRes response = paymentService.createSubscription(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (ResourceNotFoundException e) {
      log.error("구독 생성 오류: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    } catch (IllegalArgumentException e) {
      log.error("구독 생성 오류: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    }
  }

  @Operation(summary = "구독 조회", description = "특정 구독 정보를 조회합니다.")
  @GetMapping("/{id}")
  public ResponseEntity<SubscriptionInfoRes> getSubscription(@PathVariable("id") Long subscriptionId) {
    try {
      SubscriptionInfoRes response = paymentService.getSubscription(subscriptionId);
      return ResponseEntity.ok(response);
    } catch (ResourceNotFoundException e) {
      log.error("구독 조회 오류: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(summary = "사용자별 구독 목록 조회", description = "사용자의 모든 구독을 조회합니다.")
  @GetMapping("/user/{userId}")
  public ResponseEntity<SubscriptionResponse.SubscriptionListRes> getSubscriptionsByUserId(@PathVariable("userId") Long userId) {
    SubscriptionResponse.SubscriptionListRes response = paymentService.getSubscriptionsByUserId(userId);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "구독 취소", description = "구독을 취소합니다.")
  @PostMapping("/cancel")
  public ResponseEntity<Void> cancelSubscription(
      @Valid @RequestBody SubscriptionRequest.CancelSubscriptionReq request) {

    log.info("구독 취소 요청: subscriptionId={}", request.subscriptionId());
    try {
      boolean success = paymentService.cancelSubscription(request);
      return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    } catch (ResourceNotFoundException e) {
      log.error("구독 취소 오류: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(summary = "자동 갱신 설정 변경", description = "구독의 자동 갱신 설정을 변경합니다.")
  @PutMapping("/{id}/auto-renew")
  public ResponseEntity<Void> updateAutoRenew(
      @PathVariable("id") Long subscriptionId,
      @RequestParam("value") boolean autoRenew) {

    log.info("자동 갱신 설정 변경 요청: subscriptionId={}, autoRenew={}", subscriptionId, autoRenew);
    try {
      boolean success = paymentService.updateAutoRenew(subscriptionId, autoRenew);
      return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    } catch (ResourceNotFoundException e) {
      log.error("자동 갱신 설정 변경 오류: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(summary = "구독별 결제 내역 조회", description = "특정 구독의 모든 결제 내역을 조회합니다.")
  @GetMapping("/{id}/payments")
  public ResponseEntity<SubscriptionResponse.PaymentHistoryListRes> getPaymentHistoriesBySubscriptionId(
      @PathVariable("id") Long subscriptionId) {

    SubscriptionResponse.PaymentHistoryListRes response = paymentService.getPaymentHistoriesBySubscriptionId(subscriptionId);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "결제 취소", description = "특정 결제를 취소합니다.")
  @PostMapping("/payments/cancel")
  public ResponseEntity<Void> cancelPayment(
      @Valid @RequestBody SubscriptionRequest.CancelPaymentReq request) {

    log.info("결제 취소 요청: merchantUid={}", request.merchantUid());
    try {
      boolean success = paymentService.cancelPayment(request);
      return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    } catch (ResourceNotFoundException e) {
      log.error("결제 취소 오류: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    }
  }
}
