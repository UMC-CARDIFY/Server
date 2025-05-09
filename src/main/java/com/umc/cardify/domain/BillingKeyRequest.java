package com.umc.cardify.domain;

import com.umc.cardify.domain.enums.BillingKeyStatus;
import jakarta.persistence.*;
import lombok.*;

// 정기 결제(빌링키 및 고객 번호) 관련 저장용 엔터티
@Entity
@Table(name = "billing_key_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class BillingKeyRequest extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, unique = true)
  private String merchantUid; // 주문 번호

  @Column(nullable = false)
  private String customerUid; // 고객 번호

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Setter
  private BillingKeyStatus status;

  @Column(nullable = false)
  private String pgProvider;

  @Column(columnDefinition = "TEXT")
  private String requestData;

  @Column
  @Setter
  private String pgToken;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_method_id")
  @Setter
  private PaymentMethod paymentMethod;

  public void updateStatus(BillingKeyStatus status) {
    this.status = status;
  }
}