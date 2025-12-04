package com.umc.cardify.controller;

import com.umc.cardify.dto.payment.webhook.WebhookRequest;
import com.umc.cardify.service.payment.SimplePayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
@Tag(name = "WebhookController", description = "결제 웹훅 처리 API")
public class WebhookController {

  private final SimplePayService simplePayService;

  @Operation(summary = "결제 웹훅 처리", description = "포트원/카카오페이로부터 전송되는 결제 상태 웹훅을 처리합니다.")
  @PostMapping("/payment")
  public ResponseEntity<Void> handlePaymentWebhook(@RequestBody WebhookRequest request) {
    log.info("결제 웹훅 수신: impUid={}, merchantUid={}, status={}",
        request.getImp_uid(), request.getMerchant_uid(), request.getStatus());

    if (request.getImp_uid() == null || request.getMerchant_uid() == null) {
      log.error("웹훅 요청 데이터 오류: 필수 항목 누락");
      return ResponseEntity.badRequest().build();
    }

    try {
      simplePayService.handleWebhook(request);
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      log.error("웹훅 처리 중 오류 발생: {}", e.getMessage());
      // 웹훅은 항상 200 OK로 응답하여 재시도를 방지
      return ResponseEntity.ok().build();
    }
  }
}