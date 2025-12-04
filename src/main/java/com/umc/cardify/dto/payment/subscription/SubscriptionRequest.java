package com.umc.cardify.dto.payment.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public class SubscriptionRequest {

  @Builder
  @Schema(title = "SUBSCRIPTION_REQ_01 : 구독 신청 요청")
  public record CreateSubscriptionReq(
      @NotNull(message = "사용자 ID는 필수입니다.")
      @Schema(description = "사용자 ID", example = "1")
      Long userId,

      @NotNull(message = "상품 ID는 필수입니다.")
      @Schema(description = "상품 ID", example = "3")
      Long productId,

      @NotNull(message = "결제 수단 ID는 필수입니다.")
      @Schema(description = "결제 수단 ID", example = "2")
      Long paymentMethodId,

      @NotNull(message = "pg사는 필수입니다.")
      @Schema(description = "pg사", example = "CARD, KAKAO, NAVER, TOSS")
      String pgProvider,


      @Schema(description = "자동 갱신 여부", example = "true")
      boolean autoRenew
  ) {}

  @Builder
  @Schema(title = "SUBSCRIPTION_REQ_02 : 구독 취소 요청")
  public record CancelSubscriptionReq(
      @NotNull(message = "구독 ID는 필수입니다.")
      @Schema(description = "구독 ID", example = "5")
      Long subscriptionId,

      @Schema(description = "취소 사유", example = "서비스 불만족")
      String cancelReason
  ) {}

  @Builder
  @Schema(title = "SUBSCRIPTION_REQ_03 : 결제 취소 요청")
  public record CancelPaymentReq(
      @NotBlank(message = "결제 고유번호는 필수입니다.")
      @Schema(description = "결제 고유번호", example = "payment_12345abcde")
      String merchantUid,

      @Schema(description = "취소 사유", example = "상품 불만족")
      String reason
  ) {}
}