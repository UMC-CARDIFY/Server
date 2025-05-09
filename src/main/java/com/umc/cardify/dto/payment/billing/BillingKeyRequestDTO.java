package com.umc.cardify.dto.payment.billing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public class BillingKeyRequestDTO {

  @Builder
  @Schema(title = "BILLING_REQ_01 : 빌링키 발급 요청")
  public record RequestBillingKeyReq(
      @NotNull(message = "사용자 ID는 필수입니다.")
      @Schema(description = "사용자 ID", example = "1")
      Long userId,

      @NotNull(message = "상품 ID는 필수입니다.")
      @Schema(description = "상품 ID", example = "1")
      Long productId,

      @Email(message = "유효한 이메일 형식이 아닙니다.")
      @Schema(description = "사용자 이메일", example = "user@example.com")
      String email,

      @NotBlank(message = "사용자 이름은 필수입니다.")
      @Schema(description = "사용자 이름", example = "홍길동")
      String name,

      @Schema(description = "콜백 URL (미지정시 기본값 사용)", example = "http://localhost:5173/archive")
      String callbackUrl
  ) {}

  // FIXME : 삭제해도 될 거 같음
  @Builder
  @Schema(title = "BILLING_REQ_02 : 빌링키 검증 및 저장 요청")
  public record VerifyBillingKeyReq(
      @NotBlank(message = "고객 식별자는 필수입니다.")
      @Schema(description = "고객 식별자", example = "customer_1_1620000000000")
      String customerUid,

      @NotNull(message = "사용자 ID는 필수입니다.")
      @Schema(description = "사용자 ID", example = "1")
      Long userId,

      @NotNull(message = "상품 ID는 필수입니다.")
      @Schema(description = "상품 ID", example = "1")
      Long productId,

      @Schema(description = "카드 정보 (JSON 문자열)", example = "{\"card_number\":\"1234-xxxx-xxxx-5678\",\"card_name\":\"신한카드\"}")
      String cardInfo
  ) {}

  @Builder
  @Schema(title = "BILLING_REQ_03 : 빌링키 발급 승인 요청")
  public record ApproveBillingKeyReq(
      @NotBlank(message = "PG 토큰은 필수입니다.")
      @Schema(description = "PG 토큰", example = "b679084e-a5ac-4c97-b8b3-2d40413d6a5e")
      String pgToken,

      @NotBlank(message = "상점 주문번호는 필수입니다.")
      @Schema(description = "상점 주문번호", example = "subscribe_0146c0c1-47d4-45d7-bca3-aa6f55d45d49")
      String merchantUid,

      @Schema(description = "고객 식별자", example = "customer_1_1620000000000")
      String customerUid,

      @Schema(description = "거래 ID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
      String tid,

      @NotNull(message = "상품 ID는 필수입니다.")
      @Schema(description = "상품 ID", example = "1")
      Long productId,

      @NotNull(message = "사용자 ID는 필수입니다.")
      @Schema(description = "사용자 ID", example = "1")
      Long userId
  ) {}
}