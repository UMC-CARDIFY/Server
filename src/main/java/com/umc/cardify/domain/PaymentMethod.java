package com.umc.cardify.domain;

import com.umc.cardify.domain.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod extends BaseEntity{

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    @Setter
    @Getter
    @Column(nullable = false)
    private String provider;

    @Setter
    @Column
    private String cardNumber;

    @Setter
    @Column
    private LocalDate validUntil;

    @Column(nullable = false)
    @Setter
    private Boolean isDefault;

    @Setter
    @Column
    private LocalDateTime deletedAt;

    // 부가 정보 저장
    @Setter
    @Column(name = "meta_data", columnDefinition = "json")
    private String metaData;

    // 정기 결제를 위한 빌링키
    @Column(name = "billing_key")
    @Setter
    private String billingKey;

    @OneToMany(mappedBy = "paymentMethod")
    private List<SubscriptionPayment> payments = new ArrayList<>();

}
