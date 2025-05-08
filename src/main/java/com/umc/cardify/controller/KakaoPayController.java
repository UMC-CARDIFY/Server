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

  @PostMapping("/billing-key/approve")
  public ResponseEntity<?> approveBillingKey(@RequestBody BillingKeyRequestDTO.ApproveBillingKeyReq request) {
    log.info("빌링키 승인 요청: pgToken={}, merchantUid={}, customerUid={}",
        request.pgToken(), request.merchantUid(), request.customerUid());

    try {
      // 빌링키 승인 처리
      BillingKeyResponse.ApproveBillingKeyRes response =
          kakaoPaymentServiceImpl.approveBillingKey(request);

      log.info("빌링키 승인 성공: customerUid={}", response.customerUid());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("빌링키 승인 처리 오류: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of(
              "success", false,
              "error", e.getMessage()
          ));
    }
  }

  @GetMapping("/success")
  public ResponseEntity<?> billingKeySuccess(
      @RequestParam("pg_token") String pgToken,
      @RequestParam(value = "merchant_uid", required = false) String merchantUid,
      @RequestParam(value = "customer_uid", required = false) String customerUid) {

    log.info("빌링키 발급 콜백: pgToken={}, merchantUid={}, customerUid={}",
        pgToken, merchantUid, customerUid);

    // 프론트엔드로 데이터 전달을 위한 HTML 페이지 생성
    String html = "<!DOCTYPE html>"
        + "<html><head><title>결제 성공</title><meta charset=\"UTF-8\"></head>"
        + "<body>"
        + "<script>"
        + "  window.opener.postMessage({"
        + "    status: 'success',"
        + "    pgToken: '" + pgToken + "',"
        + "    merchantUid: '" + merchantUid + "',"
        + "    customerUid: '" + customerUid + "'"
        + "  }, '*');"
        + "  setTimeout(function() { window.close(); }, 1000);"
        + "</script>"
        + "<h2>결제가 성공적으로 완료되었습니다.</h2>"
        + "<p>잠시 후 창이 닫힙니다.</p>"
        + "</body></html>";

    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .header("Content-Type", "text/html; charset=UTF-8")
        .body(html);
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