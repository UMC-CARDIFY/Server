package com.umc.cardify.dto.subscription.card;
import com.umc.cardify.domain.PaymentMethod;
import com.umc.cardify.domain.enums.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// TODO : 이후 디자인에 보고 수정
@Data
public class PaymentMethodResponse {

    @Builder
    @Schema(title = "PAYMENTMETHOD_RES_01 : 결제 등록 응답")
    public record registerPaymentMethodRes(
            @Schema(description = "결제 수단 ID", example = "1")
            Long id,
            @Schema(description = "결제 수단 유형", example = "CARD")
            PaymentType type,

            @Schema(description = "카드 제공사", example = "신한은행")
            String provider,

            @Schema(description = "마스킹된 카드번호", example = "123456******3456")
            String cardNumber,

            @Schema(description = "기본 결제 수단 여부", example = "true")
            boolean isDefault,

            @Schema(description = "생성 일시", example = "2025-03-27T15:30:45")
            LocalDateTime createdAt
    ) {
        // PaymentMethod 객체로부터 registerPaymentMethodRes 생성하는 생성자 추가
        public registerPaymentMethodRes(PaymentMethod paymentMethod) {
            this(
                    paymentMethod.getId(),
                    paymentMethod.getType(),
                    paymentMethod.getProvider(),
                    paymentMethod.getCardNumber(),
                    paymentMethod.getIsDefault(),
                    paymentMethod.getCreatedAt()
            );
        }
    }

}