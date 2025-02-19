package com.umc.cardify.domain;


import com.umc.cardify.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private String merchantUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private Integer amount;

    private LocalDateTime paidAt;

    @Column(nullable = false)
    private String pgProvider;

    private String failReason;

    @Builder
    public SubscriptionPayment(Subscription subscription, PaymentMethod paymentMethod,
                               String merchantUid, PaymentStatus status, Integer amount,
                               String pgProvider) {
        this.subscription = subscription;
        this.paymentMethod = paymentMethod;
        this.merchantUid = merchantUid;
        this.status = status;
        this.amount = amount;
        this.pgProvider = pgProvider;
    }
}