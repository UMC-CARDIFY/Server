package com.umc.cardify.dto.payment.subscription;

import com.umc.cardify.domain.enums.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class SubscriptionResponse {

  @Builder
  @Schema(title = "SUBSCRIPTION_RES_01 : 구독 정보 응답")
  public record SubscriptionInfoRes(
      @Schema(description = "구독 ID", example = "1")
      Long id,

      @Schema(description = "사용자 ID", example = "1")
      Long userId,

      @Schema(description = "상품 ID", example = "3")
      Long productId,

      @Schema(description = "상품명", example = "프리미엄 멤버십")
      String productName,

      @Schema(description = "구독 상태", example = "ACTIVE")
      String status,

      @Schema(description = "구독 시작일", example = "2023-05-01T10:30:00")
      LocalDateTime startDate,

      @Schema(description = "다음 결제일", example = "2023-06-01T10:30:00")
      LocalDateTime nextPaymentDate,

      @Schema(description = "자동 갱신 여부", example = "true")
      Boolean autoRenew,

      @Schema(description = "사용중인 결제 수단 정보")
      PaymentMethodInfo paymentMethod
  ) {
    @Builder
    public record PaymentMethodInfo(
        @Schema(description = "결제 수단 ID", example = "2")
        Long id,

        @Schema(description = "결제 수단 유형", example = "KAKAO")
        String type,

        @Schema(description = "결제사", example = "KAKAO")
        String provider,

        @Schema(description = "마스킹된 카드 번호", example = "123456******3456")
        String cardNumber
    ) {
    }
  }

  @Builder
  @Schema(title = "SUBSCRIPTION_RES_02 : 구독 목록 응답")
  public record SubscriptionListRes(
      @Schema(description = "구독 목록")
      List<SubscriptionInfoRes> subscriptions,

      @Schema(description = "전체 구독 수", example = "2")
      int totalCount
  ) {}

  @Builder
  @Schema(title = "SUBSCRIPTION_RES_03 : 결제 내역 정보")
  public record PaymentHistoryRes(
      @Schema(description = "결제 ID", example = "1")
      Long id,

      @Schema(description = "구독 ID", example = "1")
      Long subscriptionId,

      @Schema(description = "결제 수단 ID", example = "2")
      Long paymentMethodId,

      @Schema(description = "결제 고유번호", example = "payment_12345abcde")
      String merchantUid,

      @Schema(description = "결제 상태", example = "PAID")
      String status,

      @Schema(description = "결제 금액", example = "9900")
      int amount,

      @Schema(description = "결제일시", example = "2023-05-01T10:30:00")
      LocalDateTime paidAt,

      @Schema(description = "결제사", example = "KAKAO")
      String pgProvider
  ) {
  }

  @Builder
  @Schema(title = "SUBSCRIPTION_RES_04 : 결제 내역 목록 응답")
  public record PaymentHistoryListRes(
      @Schema(description = "결제 내역 목록")
      List<PaymentHistoryRes> payments,

      @Schema(description = "전체 결제 내역 수", example = "5")
      int totalCount
  ) {}
}
