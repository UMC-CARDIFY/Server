package com.umc.cardify.dto.payment.card;
import com.umc.cardify.domain.PaymentMethod;
import com.umc.cardify.domain.enums.PaymentType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentMethodResponse {
    private Long id;
    private PaymentType type;
    private String provider;
    private String cardNumber; // 마스킹된 카드번호
    private boolean isDefault;
    private LocalDateTime createdAt;

    public PaymentMethodResponse(PaymentMethod paymentMethod) {
        this.id = paymentMethod.getId();
        this.type = paymentMethod.getType();
        this.provider = paymentMethod.getProvider();
        this.cardNumber = paymentMethod.getCardNumber();
        this.isDefault = paymentMethod.getIsDefault();
        this.createdAt = paymentMethod.getCreatedAt();
    }
}