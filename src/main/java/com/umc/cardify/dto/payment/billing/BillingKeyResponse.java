package com.umc.cardify.dto.payment.billing;

import com.umc.cardify.domain.PaymentMethod;
import com.umc.cardify.domain.enums.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public class BillingKeyResponse {

  @Builder
  @Schema(title = "BILLING_RES_01 : 빌링키 발급 요청 응답")
  public record RequestBillingKeyRes(
      @Schema(description = "고유 주문번호", example = "subscription_f4b5e6c8-1234-5678-90ab-cdef12345678")
      String merchantUid,

      @Schema(description = "고객 식별자", example = "customer_1_1620000000000")
      String customerUid,

      @Schema(description = "요청 데이터")
      Object requestData
  ) {}

  @Builder
  @Schema(title = "BILLING_RES_02 : 빌링키 검증 및 저장 응답")
  public record VerifyBillingKeyRes(
      @Schema(description = "결제 수단 ID", example = "1")
      Long id,

      @Schema(description = "사용자 ID", example = "1")
      Long userId,

      @Schema(description = "결제 수단 유형", example = "CARD")
      String type,

      @Schema(description = "결제사", example = "KAKAO")
      String provider,

      @Schema(description = "기본 결제 수단 여부", example = "true")
      Boolean isDefault
  ) {}

  @Builder
  @Schema(title = "BILLING_RES_03 : 빌링키 검증 및 저장 응답")
  public record KakaoPayMethodRes(
      @Schema(description = "결제 수단 ID", example = "1")
      Long id,

      @Schema(description = "결제 수단 유형", example = "CARD")
      PaymentType type,

      @Schema(description = "결제사", example = "KAKAO (신한카드)")
      String provider,

      @Schema(description = "마스킹된 카드 정보", example = "123456******3456")
      String cardNumber,

      @Schema(description = "기본 결제 수단 여부", example = "true")
      Boolean isDefault,

      @Schema(description = "등록 일시", example = "2025-03-27T15:30:45")
      LocalDateTime createdAt
  ) {
    // PaymentMethod 객체로부터 KakaoPayMethodRes 생성하는 생성자 추가
    public static KakaoPayMethodRes fromPaymentMethod(PaymentMethod paymentMethod) {
      return KakaoPayMethodRes.builder()
          .id(paymentMethod.getId())
          .type(paymentMethod.getType())
          .provider(paymentMethod.getProvider())
          .cardNumber(paymentMethod.getCardNumber())
          .isDefault(paymentMethod.getIsDefault())
          .createdAt(paymentMethod.getCreatedAt())
          .build();
    }
  }

  @Builder
  @Schema(title = "BILLING_RES_04 : 빌링키 발급 승인 응답")
  public record ApproveBillingKeyRes(
      @Schema(description = "상점 주문번호", example = "subscribe_0146c0c1-47d4-45d7-bca3-aa6f55d45d49")
      String merchantUid,

      @Schema(description = "고객 식별자", example = "customer_1_1620000000000")
      String customerUid,

      @Schema(description = "상태", example = "success")
      String status,

      @Schema(description = "결제 수단 ID", example = "1")
      Long paymentMethodId,

      @Schema(description = "사용자 ID", example = "1")
      Long userId,

      @Schema(description = "구독 ID", example = "1")
      Long subscriptionId
  ) {}

  @Builder
  @Schema(title = "BILLING_RES_05 : 빌링키 상태 조회 응답")
  public record BillingStatusRes(
      @Schema(description = "상점 주문번호", example = "subscribe_0146c0c1-47d4-45d7-bca3-aa6f55d45d49")
      String merchantUid,

      @Schema(description = "고객 식별자", example = "customer_1_1620000000000")
      String customerUid,

      @Schema(description = "상태", example = "paid")
      String status
  ) {}
}