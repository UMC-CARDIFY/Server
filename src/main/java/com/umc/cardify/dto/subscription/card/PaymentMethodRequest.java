package com.umc.cardify.dto.subscription.card;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class PaymentMethodRequest {
    @NotBlank(message = "고객 식별자는 필수입니다")
    private String customerUid;

    @NotBlank(message = "카드 이름은 필수입니다")
    private String cardName;

    @NotBlank(message = "카드 번호는 필수입니다")
    private String cardNumber;

    @NotBlank(message = "카드 제공사는 필수입니다")
    private String cardProvider;

    private boolean isDefault = false;
}