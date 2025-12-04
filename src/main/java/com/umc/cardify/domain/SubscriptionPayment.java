package com.umc.cardify.domain;


import com.umc.cardify.domain.enums.PaymentStatus;
import com.umc.cardify.domain.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@Entity
@Getter
@NoArgsConstructor
public class SubscriptionPayment extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Setter
    @Column(nullable = false)
    private String merchantUid;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Setter
    @Column(nullable = false)
    private Integer amount;

    @Setter
    @Column
    private LocalDateTime paidAt;

    @Setter
    @Column(nullable = false)
    private String pgProvider;

    @Setter
    @Column(name = "pg_response", columnDefinition = "TEXT")
    private String pgResponse;
}