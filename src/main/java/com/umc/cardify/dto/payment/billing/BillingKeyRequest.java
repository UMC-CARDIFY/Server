package com.umc.cardify.dto.payment.billing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public class BillingKeyRequest {

  @Builder
  @Schema(title = "BILLING_REQ_01 : 빌링키 발급 요청")
  public record RequestBillingKeyReq(
      @NotNull(message = "사용자 ID는 필수입니다.")
      @Schema(description = "사용자 ID", example = "1")
      Long userId,

      @NotNull(message = "상품 ID는 필수입니다.")
      @Schema(description = "상품 ID", example = "3")
      Long productId,

      @Email(message = "유효한 이메일 형식이 아닙니다.")
      @Schema(description = "사용자 이메일", example = "user@example.com")
      String email,

      @NotBlank(message = "사용자 이름은 필수입니다.")
      @Schema(description = "사용자 이름", example = "홍길동")
      String name
  ) {}

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
      @Schema(description = "상품 ID", example = "3")
      Long productId,

      @Schema(description = "카드 정보 (JSON 문자열)", example = "{\"card_number\":\"1234-xxxx-xxxx-5678\",\"card_name\":\"신한카드\"}")
      String cardInfo
  ) {}
}