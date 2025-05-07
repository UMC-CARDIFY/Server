package com.umc.cardify.controller;

import com.umc.cardify.config.exception.ResourceNotFoundException;
import com.umc.cardify.dto.payment.subscription.SubscriptionRequest;
import com.umc.cardify.dto.payment.subscription.SubscriptionResponse;
import com.umc.cardify.dto.payment.subscription.SubscriptionResponse.SubscriptionInfoRes;
import com.umc.cardify.service.subscription.SubscriptionServiceImpl;
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
@Tag(name = "SubscriptionController", description = "구독 관련 API")
public class SubscriptionController {

  private final SubscriptionServiceImpl subscriptionServiceImpl;

  @Operation(summary = "구독 생성", description = "새로운 구독을 생성합니다.")
  @PostMapping
  public ResponseEntity<SubscriptionInfoRes> createSubscription(
      @Valid @RequestBody SubscriptionRequest.CreateSubscriptionReq request,
      @RequestHeader("Authorization") String token) {

    log.info("구독 생성 요청: userId={}, productId={}", request.userId(), request.productId());
    try {
      SubscriptionInfoRes response = subscriptionServiceImpl.createSubscription(request, token);
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
  public ResponseEntity<SubscriptionInfoRes> getSubscription(@PathVariable("id") Long subscriptionId,
                                                             @RequestHeader("Authorization") String token) {
    try {
      SubscriptionInfoRes response = subscriptionServiceImpl.getSubscription(subscriptionId, token);
      return ResponseEntity.ok(response);
    } catch (ResourceNotFoundException e) {
      log.error("구독 조회 오류: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(summary = "사용자별 구독 목록 조회", description = "사용자의 모든 구독을 조회합니다.")
  @GetMapping("/user")
  public ResponseEntity<SubscriptionResponse.SubscriptionListRes> getSubscriptionsByUserId(@RequestHeader("Authorization") String token) {
    SubscriptionResponse.SubscriptionListRes response = subscriptionServiceImpl.getSubscriptionsByUserId(token);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "구독 취소", description = "구독을 취소합니다.")
  @PostMapping("/cancel")
  public ResponseEntity<Void> cancelSubscription(
      @Valid @RequestBody SubscriptionRequest.CancelSubscriptionReq request,
      @RequestHeader("Authorization") String token) {

    log.info("구독 취소 요청: subscriptionId={}", request.subscriptionId());
    try {
      boolean success = subscriptionServiceImpl.cancelSubscription(request, token);
      return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    } catch (ResourceNotFoundException e) {
      log.error("구독 취소 오류: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    }
  }

  // TODO :  자동 갱신 설정 변경 가능하게 할 건지 다시 확인
  @Operation(summary = "자동 갱신 설정 변경", description = "구독의 자동 갱신 설정을 변경합니다.")
  @PutMapping("/{id}/auto-renew")
  public ResponseEntity<Void> updateAutoRenew(
      @PathVariable("id") Long subscriptionId,
      @RequestParam("value") boolean autoRenew,
      @RequestHeader("Authorization") String token) {

    log.info("자동 갱신 설정 변경 요청: subscriptionId={}, autoRenew={}", subscriptionId, autoRenew);
    try {
      boolean success = subscriptionServiceImpl.updateAutoRenew(subscriptionId, autoRenew, token);
      return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    } catch (ResourceNotFoundException e) {
      log.error("자동 갱신 설정 변경 오류: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(summary = "구독별 결제 내역 조회", description = "특정 구독의 모든 결제 내역을 조회합니다.")
  @GetMapping("/{id}/payments")
  public ResponseEntity<SubscriptionResponse.PaymentHistoryListRes> getPaymentHistoriesBySubscriptionId(
      @PathVariable("id") Long subscriptionId,
      @RequestHeader("Authorization") String token) {

    SubscriptionResponse.PaymentHistoryListRes response = subscriptionServiceImpl.getPaymentHistoriesBySubscriptionId(subscriptionId, token);
    return ResponseEntity.ok(response);
  }
}
