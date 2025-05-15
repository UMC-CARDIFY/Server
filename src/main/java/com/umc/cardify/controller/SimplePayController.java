package com.umc.cardify.controller;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import com.umc.cardify.domain.BillingKeyRequest;
import com.umc.cardify.dto.payment.billing.BillingKeyRequestDTO;
import com.umc.cardify.dto.payment.billing.BillingKeyResponse;
import com.umc.cardify.dto.payment.subscription.SubscriptionRequest;
import com.umc.cardify.dto.payment.webhook.WebhookRequest;
import com.umc.cardify.repository.BillingKeyRequestRepository;
import com.umc.cardify.service.payment.SimplePayServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/simple-pay")
@RequiredArgsConstructor
@Tag(name = "SimplePayController", description = "간편결제(카카오, 토스, 네이버) 정기결제 관련 API")
public class SimplePayController {

  private final SimplePayServiceImpl simplePayServiceImpl;
  private final BillingKeyRequestRepository billingKeyRequestRepository;
  private final IamportClient iamportClient;

  @Operation(summary = "빌링키 발급 요청", description = "간편결제 정기결제를 위한 빌링키 발급을 요청합니다.")
  @PostMapping("/billing-key/request")
  public ResponseEntity<BillingKeyResponse.RequestBillingKeyRes> requestBillingKey(
      @Valid @RequestBody BillingKeyRequestDTO.RequestBillingKeyReq request) {

    log.info("간편결제 빌링키 발급 요청: userId={}, productId={}", request.userId(), request.productId());
    try {
      BillingKeyResponse.RequestBillingKeyRes response = simplePayServiceImpl.requestBillingKey(request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("간편결제 빌링키 발급 오류: {}", e.getMessage());
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
          simplePayServiceImpl.approveBillingKey(request);

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
  @Operation(summary = "결제 성공 처리", description = "결제 성공 시 리다이렉트되는 엔드포인트입니다.")
  public ResponseEntity<String> paymentSuccess(
      @RequestParam("pg_token") String pgToken,
      @RequestParam("merchant_uid") String merchantUid,
      @RequestParam(value = "customer_uid", required = false) String customerUid,
      @RequestParam(value = "imp_uid", required = false) String impUid) {

    log.info("결제 성공: pgToken={}, merchantUid={}, customerUid={}, impUid={}",
        pgToken, merchantUid, customerUid, impUid);

    // 프론트엔드로 리다이렉션할 URL
    // TODO : 배포 후 수정
    String redirectUrl = "http://localhost:5173/payment/complete" +
        "?pg_token=" + pgToken +
        "&merchant_uid=" + merchantUid +
        "&customer_uid=" + (customerUid != null ? customerUid : "") +
        "&imp_uid=" + (impUid != null ? impUid : "");

    return ResponseEntity.ok("Success! Redirecting to: " + redirectUrl);
  }

  @GetMapping("/fail")
  @Operation(summary = "결제 실패 처리", description = "결제 실패 시 리다이렉트되는 엔드포인트입니다.")
  public ResponseEntity<String> paymentFail(
      @RequestParam("merchant_uid") String merchantUid,
      @RequestParam(value = "error_msg", required = false) String errorMsg) {

    log.info("결제 실패: merchantUid={}, errorMsg={}", merchantUid, errorMsg);

    // 프론트엔드로 리다이렉션할 URL
    // TODO : 배포 후 수정
    String redirectUrl = "http://localhost:5173/payment/fail" +
        "?merchant_uid=" + merchantUid +
        "&error_msg=" + (errorMsg != null ? errorMsg : "결제에 실패했습니다.");

    return ResponseEntity.ok("Failed! Redirecting to: " + redirectUrl);
  }

  // 빌링키 승인 결과 확인 API (프론트엔드에서 호출)
  @GetMapping("/billing-status/{merchantUid}")
  public ResponseEntity<?> getBillingStatus(@PathVariable String merchantUid) {
    try {
      BillingKeyResponse.BillingStatusRes status = simplePayServiceImpl.getBillingStatus(merchantUid);
      return ResponseEntity.ok(status);
    } catch (Exception e) {
      log.error("빌링키 상태 조회 오류: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @Operation(summary = "결제 취소", description = "간편 결제를 취소합니다.")
  @PostMapping("/cancel")
  public ResponseEntity<Void> cancelPayment(
      @Valid @RequestBody SubscriptionRequest.CancelPaymentReq request) {

    log.info("간편 결제 취소 요청: merchantUid={}", request.merchantUid());
    try {
      boolean success = simplePayServiceImpl.cancelPayment(request);
      return success
          ? ResponseEntity.ok().build()
          : ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    } catch (Exception e) {
      log.error("간편 결제 취소 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Operation(summary = "웹훅 처리", description = "카카오페이, 토스, 네이버/포트원에서 전송하는 결제 웹훅을 처리합니다.")
  @PostMapping("/webhook")
  public ResponseEntity<Void> handleWebhook(@RequestBody WebhookRequest request) {

    log.info("간편결제 웹훅 수신: impUid={}, merchantUid={}, status={}",
        request.getImp_uid(), request.getMerchant_uid(), request.getStatus());
    try {
      simplePayServiceImpl.handleWebhook(request);
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      log.error("간편결제 웹훅 처리 오류: {}", e.getMessage());
      // 웹훅은 항상 200 OK 응답 (재시도 방지)
      return ResponseEntity.ok().build();
    }
  }

  // SimplePayController.java에 추가
  @GetMapping("/billing-key/check-status")
  public ResponseEntity<?> checkBillingKeyStatus(@RequestParam String impUid) {
    try {
      // 포트원 API를 통해 결제 정보 조회
      IamportResponse<Payment> paymentResponse = iamportClient.paymentByImpUid(impUid);
      Payment payment = paymentResponse.getResponse();

      if (payment == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", "결제 정보를 찾을 수 없습니다."));
      }

      // BillingKeyRequest 정보 조회
      Optional<BillingKeyRequest> billingKeyRequestOpt =
          billingKeyRequestRepository.findByMerchantUid(payment.getMerchantUid());

      Map<String, Object> response = new HashMap<>();
      response.put("payment", payment);

      if (billingKeyRequestOpt.isPresent()) {
        BillingKeyRequest billingKeyRequest = billingKeyRequestOpt.get();
        response.put("billingKeyRequest", Map.of(
            "id", billingKeyRequest.getId(),
            "merchantUid", billingKeyRequest.getMerchantUid(),
            "customerUid", billingKeyRequest.getCustomerUid(),
            "status", billingKeyRequest.getStatus().name()
        ));
      }

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("빌링키 상태 조회 중 오류: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message", "빌링키 상태 조회 중 오류 발생: " + e.getMessage()));
    }
  }
}