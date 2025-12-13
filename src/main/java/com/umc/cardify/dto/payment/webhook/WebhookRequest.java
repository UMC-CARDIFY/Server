package com.umc.cardify.dto.payment.webhook;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(title = "WEBHOOK_01 : 결제 웹훅 요청")
public class WebhookRequest {

  @Schema(description = "아임포트 결제 고유번호", example = "imp_123456789012")
  private String imp_uid;

  @Schema(description = "가맹점 주문번호", example = "payment_12345abcde")
  private String merchant_uid;

  @Schema(description = "결제 상태", example = "paid")
  private String status;

  @Schema(description = "결제 시각", example = "1624323612")
  private String paid_at;

  @Schema(description = "영수증 URL", example = "https://example.com/receipt/12345")
  private String receipt_url;

  @Schema(description = "결제 방식", example = "card")
  private String pay_method;

  @Schema(description = "결제 금액", example = "9900")
  private Integer amount;

  @Schema(description = "결제 취소 금액", example = "0")
  private Integer cancel_amount;

  @Schema(description = "카드 정보")
  private CardInfo card_info;

  @Getter
  @Setter
  @NoArgsConstructor
  public static class CardInfo {
    @Schema(description = "카드 번호", example = "123456******3456")
    private String card_number;

    @Schema(description = "카드 유형", example = "신용")
    private String card_type;

    @Schema(description = "카드 발급사", example = "신한")
    private String card_name;

    @Schema(description = "할부 개월 수", example = "0")
    private Integer card_quota;

    @Schema(description = "카드사 승인번호", example = "12345678")
    private String card_approve_no;
  }
}
