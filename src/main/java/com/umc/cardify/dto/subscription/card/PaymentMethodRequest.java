package com.umc.cardify.dto.subscription.card;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

public class PaymentMethodRequest {

    @Builder
    public record registerPaymentReq(
            @NotBlank(message = "고객 식별자는 필수입니다")
            @Schema(description = "고객 식별자", example = "cust_12345abcde")
            String customerUid,

            @NotBlank(message = "카드 이름은 필수입니다")
            @Schema(description = "카드 이름", example = "신한카드")
            String cardName,

            @NotBlank(message = "카드 번호는 필수입니다")
            @Schema(description = "카드 번호", example = "1234-5678-9012-3456")
            String cardNumber,

            @NotBlank(message = "카드 제공사는 필수입니다")
            @Schema(description = "카드 제공사", example = "신한은행")
            String cardProvider,

            @Schema(description = "기본 결제 수단 여부", example = "true")
            boolean isDefault
    ) {}

}